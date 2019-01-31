package org.fortiss.uaserver.common;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.UaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.model.nodes.objects.ServerNode;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.client.config.UaTcpStackClientConfig;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.application.CertificateManager;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.ApplicationType;
import org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.FindServersRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.FindServersResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterServerRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterServerResponse;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisteredServer;
import org.eclipse.milo.opcua.stack.core.types.structured.RequestHeader;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.fortiss.heartbeat.HeartbeatClient;
import org.fortiss.uaserver.common.security.KeyStoreLoader;
import org.fortiss.uaserver.device.DeviceUaNamespace;
import org.fortiss.uaserver.device.X509IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * Created by profanter on 08.09.16.
 */
public abstract class MsbGenericComponent {

    private Timer periodicRegisterTimer = new Timer();
    /**
     * Period (in seconds) for executing the server registration.
     */
    private static final long PERIODIC_REGISTER_TIME_IN_SEC = 300L;
    private static final String LDS_APPLICATION_URI = "urn:fortiss:opcua:discovery:server";

    protected final Logger logger = LoggerFactory.getLogger(MsbGenericComponent.class);

    private OpcUaClient clientRegister;
    private final AtomicLong requestHandle = new AtomicLong(1L);
    private static final AtomicLong staticRequestHandle = new AtomicLong(1L);

    private URL fullEndpointUrl;
    private String serverName;

    private HeartbeatClient heartbeatClient;
    private OpcUaClient msbClient = null;

    private boolean running = false;

    private RegisterServerRequest periodicRegServerRequest;

    protected abstract String getApplicationUri();

    protected abstract String getProductUri();

    protected abstract LocalizedText getApplicationName();

    public abstract OpcUaServer getServer();

    public String getServerName() {
        return serverName;
    }

    public static void setLoggerFile(String endpointUrl) {
        Date now = new Date();
        String dayFormat = (new SimpleDateFormat("yyyy_MM_dd")).format(now);
        String timeFormat = (new SimpleDateFormat("HH-mm")).format(now);
        System.setProperty("logfile.name", dayFormat + "-device-unknown_" + timeFormat);
        URLStreamHandlerFactory factory = null;
        try {
            factory = protocol -> "opc.tcp".equals(protocol) ? new URLStreamHandler() {
                protected URLConnection openConnection(URL url) throws IOException {
                    return new URLConnection(url) {
                        public void connect() throws IOException {
                            // do nothing
                        }
                    };
                }
            } : null;
            URL.setURLStreamHandlerFactory(factory);
        } catch (final Error e) {
            // Force it via reflection
            try {
                final Field factoryField = URL.class.getDeclaredField("factory");
                factoryField.setAccessible(true);
                factoryField.set(null, factory);
            } catch (NoSuchFieldException | IllegalAccessException e1) {
                throw new Error("Could not access factory field on URL class: {}", e);
            }
        }
        URL fullEndpointUrl;
        try {
            fullEndpointUrl = new URL(endpointUrl);
        } catch (MalformedURLException e) {
            System.err.println("Invalid endpoint URL: " + endpointUrl);
            return;
        }
        if (fullEndpointUrl.getPath().length() > 1) {
            System.setProperty("logfile.name",
                    dayFormat + "-device-" + fullEndpointUrl.getPath().substring(1) + "_" + timeFormat);
        }

    }

    public MsbGenericComponent(String endpointUrl) {

        try {
            fullEndpointUrl = new URL(endpointUrl);
        } catch (MalformedURLException e) {
            logger.error("Invalid endpoint URL: " + endpointUrl, e);
            return;
        }

        serverName = fullEndpointUrl.getPath();
        if (serverName.startsWith("/") && serverName.length() > 1) {
            serverName = serverName.substring(1);
        } else {
            serverName = endpointUrl;
        }

        logger.info("Binding server on: " + fullEndpointUrl.toString());
    }

    public static CompletableFuture<Integer> getNamespaceIndex(OpcUaClient client, String namespaceUri) {

        CompletableFuture<Integer> promiseNamespaceIdx = new CompletableFuture<>();

        CompletableFuture<UaClient> tmpFuture = client.connect();
        tmpFuture.whenComplete((opcUaClient, throwable2) -> {
            if (throwable2 != null) {
                promiseNamespaceIdx.completeExceptionally(throwable2);
                return;
            }
            client.getAddressSpace().getObjectNode(Identifiers.Server, ServerNode.class)
                    .whenComplete((serverNode, throwable) -> {
                        if (throwable != null) {
                            promiseNamespaceIdx.completeExceptionally(throwable);
                            return;
                        }
                        serverNode.getNamespaceArray().whenComplete((strings, throwable1) -> {
                            if (throwable1 != null) {
                                promiseNamespaceIdx.completeExceptionally(throwable1);
                                return;
                            }
                            int idx = java.util.Arrays.asList(strings).indexOf(namespaceUri);
                            if (idx >= 0) {
                                promiseNamespaceIdx.complete(idx);
                            } else {
                                promiseNamespaceIdx.completeExceptionally(new Exception(
                                        "Namespace " + DeviceUaNamespace.NAMESPACE_URI + " not found on the server"));
                            }
                        });
                    });
        });

        return promiseNamespaceIdx;
    }

    private void startHeartbeat(String appUri) {

        CompletableFuture<Integer> promiseNamespaceIdx = getNamespaceIndex(clientRegister,
                DeviceUaNamespace.NAMESPACE_URI);

        Integer idx = null;
        try {
            idx = promiseNamespaceIdx.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        Integer port;
        Integer timeout;
        try {
            port = (Integer) clientRegister.getAddressSpace().getVariableNode(new NodeId(idx, "HeartbeatConfig/Port"))
                    .get().readValue().get().getValue().getValue();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could not initialize hearbeat client config for port.", e);
            return;
        }

        try {
            timeout = (Integer) clientRegister.getAddressSpace()
                    .getVariableNode(new NodeId(idx, "HeartbeatConfig/Timeout")).get().readValue().get().getValue()
                    .getValue();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could not initialize hearbeat client config for timeout.", e);
            return;
        }

        logger.info("Initializing HeartbeatClient with port " + port + " and timeout " + timeout);
        try {
            heartbeatClient = new HeartbeatClient(fullEndpointUrl.getHost(), port, appUri, timeout);
        } catch (SocketException | UnknownHostException e) {
            logger.error("Could not initialize hearbeat client.", e);
            return;
        }
        heartbeatClient.start();

    }

    public void stopHeartbeat() {
        heartbeatClient.close();
    }

    public void startupMdns(String mdnsName, CompletableFuture<OpcUaClient> promiseDiscoveryEndpointClient) {

        String machineIP = fullEndpointUrl.getHost();

        String bindingPort = String.valueOf(fullEndpointUrl.getPort());
        String endpointName = fullEndpointUrl.getPath();

        waitForMsbDiscovery(machineIP, bindingPort, endpointName, mdnsName, promiseDiscoveryEndpointClient);
    }

    protected OpcUaClientConfig getRegisterClientConfig(EndpointDescription endpoint) {

        X509IdentityProvider x509IdentityProvider = new X509IdentityProvider("openssl_crt.der", "herong.key");
        X509Certificate cert = x509IdentityProvider.getCertificate();
        KeyPair keyPair = new KeyPair(cert.getPublicKey(), x509IdentityProvider.getPrivateKey());
        return OpcUaClientConfig.builder().setApplicationName(LocalizedText.english("opc-ua clientRegister"))
                .setApplicationUri("urn:opcua_client").setCertificate(cert).setKeyPair(keyPair).setEndpoint(endpoint)
                .setIdentityProvider(x509IdentityProvider)
                // .setIdentityProvider(clientExample.getIdentityProvider())//
                .setRequestTimeout(uint(5000)).build();
    }

    private OpcUaClientConfig getMsbClientConfig(EndpointDescription endpoint) {
        // X509IdentityProvider x509IdentityProvider = new
        // X509IdentityProvider("openssl_crt.der",
        // "herong.key");
        // X509Certificate cert = x509IdentityProvider.getCertificate();
        // KeyPair keyPair = new KeyPair(cert.getPublicKey(),
        // x509IdentityProvider.getPrivateKey());
        return OpcUaClientConfig.builder().setApplicationName(LocalizedText.english("opc-ua clientMsb"))
                .setApplicationUri("urn:opcua_client")
                // .setCertificate(cert)
                // .setKeyPair(keyPair)
                .setEndpoint(endpoint)
                // .setIdentityProvider(x509IdentityProvider)
                // .setIdentityProvider(clientExample.getIdentityProvider())//
                .setRequestTimeout(uint(5000)).build();
    }

    protected static CompletableFuture<OpcUaClient> getClientForSuitableEndpointList(String discoveryUrls[],
            Function<EndpointDescription, OpcUaClientConfig> getClientConfig, String filterByApplicationUri,
            SecurityPolicy filterBySecurityPolicy, String filterBySecurityMode, Logger logger) {

        CompletableFuture<OpcUaClient> firstClient = new CompletableFuture<>();

        final AtomicLong endpointTryCount = new AtomicLong(0);
        Stream<String> discoveryUrlsStream = Arrays.stream(discoveryUrls);

        discoveryUrlsStream.parallel().forEach(url -> {

            CompletableFuture<OpcUaClient> tmpFuture = getClientForSuitableEndpoint(url, getClientConfig,
                    filterByApplicationUri, filterBySecurityPolicy, filterBySecurityMode, logger);

            if (firstClient.isDone()) {
                // Skip this url, we are already connected
                return;
            }

            tmpFuture.whenComplete((uaClient, throwable2) -> {
                long newCount = endpointTryCount.incrementAndGet();
                if (uaClient != null) {
                    if (!firstClient.isDone()) {
                        firstClient.complete(uaClient);
                    }
                } else {
                    logger.info("No connection on url: " + url + ". Status: " + throwable2.getMessage()
                            + " - Trying other endpoints");
                    if (newCount == discoveryUrls.length) {
                        firstClient.completeExceptionally(
                                new Exception("Could not connect to any endpoint on server " + url));
                    }
                }
            });
        });
        return firstClient;
    }

    protected static CompletableFuture<OpcUaClient> getClientForSuitableEndpoint(String discoveryUrl,
            Function<EndpointDescription, OpcUaClientConfig> getClientConfig, String filterByApplicationUri,
            SecurityPolicy filterBySecurityPolicy, String filterBySecurityMode, Logger logger) {

        CompletableFuture<EndpointDescription[]> endpoints = UaTcpStackClient.getEndpoints(discoveryUrl);
        CompletableFuture<EndpointDescription[]> endpointsFiltered = new CompletableFuture<>();

        endpoints.whenComplete((endpointDescriptions, throwable) -> {
            if (throwable != null) {
                endpointsFiltered.completeExceptionally(throwable);
                return;
            }

            Stream<EndpointDescription> endpointsStream;
            // sort by highest security level
            if (filterBySecurityPolicy == null && filterBySecurityMode == null) {
                Arrays.sort(endpointDescriptions,
                        (e1, e2) -> e2.getSecurityLevel().intValue() - e1.getSecurityLevel().intValue());
                endpointsStream = Arrays.stream(endpointDescriptions);
            } else {
                endpointsStream = Arrays.stream(endpointDescriptions);
                if (filterBySecurityMode != null) {
                    endpointsStream = endpointsStream
                            .filter(e -> e.getSecurityMode().toString().compareTo(filterBySecurityMode) == 0);
                }
                if (filterBySecurityPolicy != null) {
                    endpointsStream = endpointsStream.filter(
                            e -> e.getSecurityPolicyUri().equals(filterBySecurityPolicy.getSecurityPolicyUri()));
                }
            }
            if (filterByApplicationUri != null) {
                endpointsStream = endpointsStream
                        .filter(e -> e.getServer().getApplicationUri().compareTo(filterByApplicationUri) == 0);
            }

            EndpointDescription[] endpointsFilteredArr = endpointsStream.toArray(EndpointDescription[]::new);

            if (endpointsFilteredArr.length == 0) {

                StringBuilder sb = new StringBuilder();
                sb.append("Server '").append(discoveryUrl).append("' does not offer a suitable endpoint");
                if (filterBySecurityPolicy != null) {
                    sb.append(" [").append(filterBySecurityPolicy.name()).append("]");
                }
                if (filterBySecurityMode != null) {
                    sb.append(" [").append(filterBySecurityMode).append("]");
                }
                if (filterByApplicationUri != null) {
                    sb.append(" ApplicationUri == ").append(filterByApplicationUri);
                }
                endpointsFiltered.completeExceptionally(new Exception(sb.toString()));
            } else {
                endpointsFiltered.complete(endpointsFilteredArr);
            }
        });

        CompletableFuture<OpcUaClient> clientConnectFuture = new CompletableFuture<>();

        endpointsFiltered.whenComplete((endpointDescriptions, throwable) -> {
            if (throwable != null) {
                clientConnectFuture.completeExceptionally(throwable);
                return;
            }
            final AtomicLong endpointTryCount = new AtomicLong(0);
            Stream<EndpointDescription> endpointDescriptionStream = Arrays.stream(endpointDescriptions);

            endpointDescriptionStream.parallel().forEach(endpoint -> {
                String endpointStr = endpoint.getEndpointUrl() + " [" + endpoint.getSecurityPolicyUri() + ", "
                        + endpoint.getSecurityMode() + "]";
                if (clientConnectFuture.isDone()) {
                    // Skip this endpoint, we are already connected
                    return;
                }

                OpcUaClientConfig clientConfig = getClientConfig.apply(endpoint);
                OpcUaClient tmpClient = new OpcUaClient(clientConfig);
                logger.debug("Trying endpoint: " + endpointStr);
                CompletableFuture<UaClient> tmpFuture = tmpClient.connect();
                tmpFuture.whenComplete((uaClient, throwable2) -> {
                    long newCount = endpointTryCount.incrementAndGet();
                    if (uaClient != null) {
                        if (!clientConnectFuture.isDone()) {
                            clientConnectFuture.complete(tmpClient);
                        }
                    } else {
                        logger.info("No connection on endpoint: " + endpoint.getEndpointUrl() + ". Status: "
                                + throwable2.getMessage() + " - Trying other endpoints");
                        if (newCount == endpointDescriptions.length) {
                            clientConnectFuture.completeExceptionally(
                                    new Exception("Could not connect to any endpoint on server " + discoveryUrl));
                        }
                    }
                });
            });
        });

        return clientConnectFuture;
    }

    private void waitForMsbDiscovery(String machineIP, String bindingPort, String endpointName, String mdnsName,
            CompletableFuture<OpcUaClient> promiseLdsEndpointClient) {
        try {
            InetAddress ia = InetAddress.getByName(machineIP);
            JmDNS jmdns = JmDNS.create(ia);

            if (endpointName.length() > 2 && endpointName.startsWith("/")) {
                endpointName = endpointName.substring(1);
            }

            final String mdnsType = "_opcua-tcp._tcp.local.";

            ServiceInfo serviceInfo = ServiceInfo.create(mdnsType, mdnsName, Integer.parseInt(bindingPort),
                    "path=/" + endpointName);
            jmdns.registerService(serviceInfo);

            logger.debug("Listening for mDNS announce of type '" + mdnsType + "'");

            // Add a service listener
            jmdns.addServiceListener(mdnsType, new ServiceListener() {
                @Override
                public void serviceAdded(ServiceEvent event) {
                    // System.out.println("Service added: " + event.getInfo());
                    // force a service resolve
                    event.getDNS().getServiceInfo(event.getType(), event.getName());
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    logger.debug("mDNS service removed: " + event.getInfo());
                }

                @Override
                public void serviceResolved(ServiceEvent event) {
                    logger.debug("mDNS service resolved: " + event.getInfo());

                    InetAddress[] addressList = event.getInfo().getInetAddresses();
                    for (InetAddress mdnsIa : addressList) {

                        if (Arrays.equals(ia.getAddress(), mdnsIa.getAddress())
                                && event.getInfo().getPort() == Integer.valueOf(bindingPort)) {
                            continue; // its ourself
                        }

                        // Use Host address to avoid problem in fortiss subnet
                        String url = "opc.tcp://" + mdnsIa.getHostAddress() + ":" + event.getInfo().getPort()
                                + event.getInfo().getPropertyString("path");

                        if (url.endsWith("/")) {
                            url = url.substring(0, url.length() - 1);
                        }

                        // only accept the specific lds server for registering
                        CompletableFuture<OpcUaClient> ldsEndpoint = MsbGenericComponent.getClientForSuitableEndpoint(
                                url, endpointDescription -> getRegisterClientConfig(endpointDescription),
                                LDS_APPLICATION_URI, null, null, logger);

                        ldsEndpoint.whenComplete((opcUaClient, throwable) -> {
                            if (throwable != null) {
                                promiseLdsEndpointClient.completeExceptionally(throwable);
                            } else {
                                promiseLdsEndpointClient.complete(opcUaClient);
                            }
                        });
                    }

                }
            });
        } catch (IOException e) {
            promiseLdsEndpointClient.completeExceptionally(e);
        }

    }

    private boolean createRegisterServerData(OpcUaClient discoveryEndpointClient) {
        clientRegister = discoveryEndpointClient;

        logger.info(
                "+++++ Found available endpoint for register: " + clientRegister.getConfig().getEndpoint().toString());

        RequestHeader header = new RequestHeader(NodeId.NULL_VALUE, DateTime.now(),
                uint(requestHandle.getAndIncrement()), uint(0), null, uint(60), null);

        LocalizedText[] serverNames = new LocalizedText[1];
        serverNames[0] = getApplicationName();
        ApplicationType serverType = ApplicationType.ClientAndServer;
        String[] discoveryUrls = new String[1];
        discoveryUrls[0] = fullEndpointUrl.toString();
        RegisteredServer serverToBeRegistered = new RegisteredServer(getApplicationUri(), getProductUri(), serverNames,
                serverType, null, discoveryUrls, null, true);

        periodicRegServerRequest = new RegisterServerRequest(header, serverToBeRegistered);
        return true;
    }

    class PeriodicRegistrationManager extends TimerTask {

        public void run() {
            // CompletableFuture<RegisterServerResponse> future =
            // stackClient.sendRequest(periodicRegServerRequest);
            CompletableFuture<RegisterServerResponse> future = clientRegister.sendRequest(periodicRegServerRequest);
            future.whenComplete((response, ex) -> {
                if (response != null) {
                    logger.info("Received RegisterServerResponse output={}", response.getResponseHeader().toString());
                } else {
                    logger.error("Periodic RegisterServer failed.", ex);
                }
            });
        }
    }

    public boolean register(OpcUaClient discoveryEndpointClient) {
        if (!createRegisterServerData(discoveryEndpointClient)) {
            return false;
        }

        periodicRegisterTimer.schedule(new PeriodicRegistrationManager(), 0, PERIODIC_REGISTER_TIME_IN_SEC * 1000);
        return true;

    }

    public CompletableFuture<RegisterServerResponse> unregister() {
        periodicRegisterTimer.cancel();

        RegisteredServer serverToBeUnRegistered = new RegisteredServer(getApplicationUri(), getProductUri(),
                periodicRegServerRequest.getServer().getServerNames(),
                periodicRegServerRequest.getServer().getServerType(), null,
                periodicRegServerRequest.getServer().getDiscoveryUrls(), null, false);

        RegisterServerRequest unregServerRequest = new RegisterServerRequest(
                periodicRegServerRequest.getRequestHeader(), serverToBeUnRegistered);

        return clientRegister.sendRequest(unregServerRequest);
    }

    public void stopWaitLoop() {
        this.running = false;
    }

    public void waitUntilFinish() {
        this.running = true;

        while (this.running) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {

            }
        }
    }

    protected OpcUaServerConfig getServerConfig() throws Exception {
        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(true, // allow
                // anonymous
                // access
                challenge -> {
                    String user0 = "user";
                    String pass0 = "password";

                    char[] cs = new char[1000];
                    Arrays.fill(cs, 'a');
                    String user1 = new String(cs);
                    String pass1 = new String(cs);

                    boolean match0 = user0.equals(challenge.getUsername()) && pass0.equals(challenge.getPassword());

                    boolean match1 = user1.equals(challenge.getUsername()) && pass1.equals(challenge.getPassword());

                    return match0 || match1;
                });

        List<UserTokenPolicy> userTokenPolicies = newArrayList(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
                OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME);

        KeyStoreLoader loader = new KeyStoreLoader().load();

        // Enforce with security
        // CertificateManager certificateManager = new
        // DefaultCertificateManager(loader.getServerKeyPair(),
        // loader.getServerCertificate());
        // No security
        CertificateManager certificateManager = new DefaultCertificateManager();
        File securityDir = new File("./security/");
        if (!securityDir.exists() && !securityDir.mkdirs()) {
            throw new Exception("unable to create security directory");
        }
        CertificateValidator certificateValidator = new DefaultCertificateValidator(securityDir);

        return OpcUaServerConfig.builder().setApplicationName(getApplicationName())
                .setApplicationUri(getApplicationUri()).setBindAddresses(newArrayList(fullEndpointUrl.getHost()))
                .setBindPort(fullEndpointUrl.getPort()).setCertificateManager(certificateManager)
                .setCertificateValidator(certificateValidator)
                .setSecurityPolicies(EnumSet.of(SecurityPolicy.None, SecurityPolicy.Basic128Rsa15))
                .setProductUri(getProductUri()).setServerName(serverName).setUserTokenPolicies(userTokenPolicies)
                .setIdentityValidator(identityValidator).build();
    }

    protected static CompletableFuture<ApplicationDescription> getMsbFromLds(String ldsEndpointUrl,
            String msbApplicationUri, Logger logger) {
        KeyStoreLoader loader;
        try {
            loader = new KeyStoreLoader().load();
        } catch (Exception e) {
            CompletableFuture<ApplicationDescription> f = new CompletableFuture<>();
            f.completeExceptionally(new RuntimeException("Could not load keys"));
            return f;
        }

        UaTcpStackClientConfig config = UaTcpStackClientConfig.builder()
                .setApplicationName(LocalizedText.english("Device Adapter LDS Browser"))
                .setApplicationUri(String.format("urn:da:lds_browse:%s", UUID.randomUUID()))
                .setCertificate(loader.getClientCertificate()).setKeyPair(loader.getClientKeyPair())
                .setEndpointUrl(ldsEndpointUrl).build();

        UaTcpStackClient client = new UaTcpStackClient(config);

        RequestHeader header = new RequestHeader(NodeId.NULL_VALUE, DateTime.now(),
                uint(staticRequestHandle.getAndIncrement()), uint(0), null, uint(60), null);

        FindServersRequest request = new FindServersRequest(header, null, null, null);

        CompletableFuture<ApplicationDescription> f = new CompletableFuture<>();

        client.<FindServersResponse>sendRequest(request).whenComplete((findServersResponse, throwable) -> {
            if (throwable != null) {
                f.completeExceptionally(throwable);
            }

            StatusCode statusCode = findServersResponse.getResponseHeader().getServiceResult();

            if (statusCode.isGood()) {
                if (findServersResponse.getServers() != null) {
                    for (ApplicationDescription ad : findServersResponse.getServers()) {
                        if (ad.getApplicationUri().equals(msbApplicationUri)) {
                            f.complete(ad);
                            break;
                        }
                    }
                }
                if (!f.isDone()) {
                    logger.error("Did not find any suitable MSB server on the LDS " + ldsEndpointUrl
                            + ". Expected Application URI: " + msbApplicationUri);
                    f.complete(null);
                }
            } else {
                f.completeExceptionally(new UaException(statusCode));
            }
        });

        return f;
    }

    protected static String[] getMsbEndpointFromLds(String ldsEndpointUrl, String msbApplicationUri, Logger logger) {
        ApplicationDescription ad;
        try {
            ad = getMsbFromLds(ldsEndpointUrl, msbApplicationUri, logger).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }

        return ad.getDiscoveryUrls();

    }

    public boolean initMsbEndpoint(String ldsEndpointUrl, String msbApplicationUri) {
        String msbEndpointUrls[] = getMsbEndpointFromLds(ldsEndpointUrl, msbApplicationUri, logger);
        msbEndpointUrls[0] = "opc.tcp://200.200.200.125:12637/MSB-OPCUA-SERVER";

        CompletableFuture<OpcUaClient> msbEndpointFuture = getClientForSuitableEndpointList(msbEndpointUrls,
                this::getMsbClientConfig, null, null, null, logger);

        try {
            msbClient = msbEndpointFuture.get();
            logger.info("Connected with MSB endpoint: " + msbClient.getConfig().getEndpoint().get().getEndpointUrl());
            // open a connection when we need it again
            msbClient.disconnect();
            startHeartbeat(getApplicationUri());
            return true;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could not initialize MSB endpoint.", e);
            return false;
        }
    }

    public OpcUaClient getMsbClient() {
        return msbClient;
    }

    public CompletableFuture<CallMethodResult> changeState(String da_id, String recipe_id, String product_id,
            String product_type, Boolean searchForNextRecipe, String state, String sr_id) {
        Variant[] args = { new Variant(da_id), new Variant(recipe_id), new Variant(product_id),
                new Variant(product_type), new Variant(searchForNextRecipe), new Variant(state), new Variant(sr_id) };
        logger.info("Send changeState: DA_ID='{}' Product='{}' Recipe='{}' SearchForNextRecipe = '{}'", da_id,
                product_id, recipe_id, searchForNextRecipe);
        try {
            msbClient.connect().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Can not connect to MSB", e);
            CompletableFuture<CallMethodResult> f = new CompletableFuture<>();

            f.completeExceptionally(e);
            return f;
        }

        CompletableFuture<CallMethodResult> future = new CompletableFuture<>();
        return getMsbClient().call(new CallMethodRequest(new NodeId(2, "MSB"), new NodeId(2, "MSB/ChangeState"), args))
                .whenComplete((callMethodResult, throwable) -> {
                    msbClient.disconnect();
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(callMethodResult);
                    }
                });
    }

    public CompletableFuture<CallMethodResult> updateDevice(String deviceId) {
        Variant[] args = { new Variant(deviceId) };
        logger.info("Send updateDevice: DA_ID='{}'", deviceId);
        try {
            getMsbClient().connect().get();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Can not connect to MSB", e);
            CompletableFuture<CallMethodResult> f = new CompletableFuture<>();

            f.completeExceptionally(e);
            return f;
        }

        CompletableFuture<CallMethodResult> future = new CompletableFuture<>();
        return getMsbClient().call(new CallMethodRequest(new NodeId(2, "MSB"), new NodeId(2, "MSB/UpdateDevice"), args))
                .whenComplete((callMethodResult, throwable) -> {
                    getMsbClient().disconnect();
                    if (throwable != null) {
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(callMethodResult);
                    }
                });
    }
}
