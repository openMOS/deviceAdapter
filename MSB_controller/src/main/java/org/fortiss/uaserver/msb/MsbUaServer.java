/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.fortiss.uaserver.msb;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.application.CertificateManager;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.ApplicationDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.eclipse.milo.opcua.stack.core.types.structured.FindServersRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.GetEndpointsRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterServerRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.fortiss.heartbeat.HeartbeatServer;
import org.fortiss.uaserver.msb.security.X509IdentityProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Lists.newArrayList;

public class MsbUaServer {

    private static String MDNS_LISTENING = "_opcua-tcp._tcp.local.";

    private static final Logger logger = LoggerFactory.getLogger(MsbUaServer.class);
    private OpcUaServer server;
    private HeartbeatServer heartbeatServer;

    static String serverName = "";
    static String bindingPort = "4840";
    static String bindingIP = "127.0.0.1";

    static String mappingFile = "mapping.json";


    private final Map<String, String> uriDeviceIdMapping = new HashMap<>();
    private CompleteDiscoveryServiceSet cdss;

    public static void main(String[] args) throws ExecutionException, InterruptedException, Exception {

        MsbUaServer s;
        if (args.length != 0) {
            s = new MsbUaServer(args[0]);

        } else {
            s = new MsbUaServer();

        }

        // Create mDNS for binding
        Thread mDNS = new Thread(new StartMdnsService());
        mDNS.start();

        // Create OPC UA discovery server
        s.startWaitUntilFinish();

    }

    private static class StartMdnsService
        implements Runnable {

        public void run() {
            JmDNS jmdns = null;
            try {

                InetAddress ia = InetAddress.getByName(bindingIP); // Create a JmDNS instance
                jmdns = JmDNS.create(ia);

                // Register a service
                ServiceInfo serviceInfo = ServiceInfo
                    .create(MDNS_LISTENING, "Java MSB", Integer.parseInt(bindingPort), "path=/" + serverName);
                jmdns.registerService(serviceInfo);

                jmdns.addServiceListener(MDNS_LISTENING, new ServiceListener() {

                    @Override
                    public void serviceResolved(ServiceEvent event) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void serviceRemoved(ServiceEvent event) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void serviceAdded(ServiceEvent event) {
                        // TODO handle the connection after getting the ip
                        ServiceInfo info = event.getDNS().getServiceInfo(event.getType(), event.getName());
                        logger.debug(info.toString());
                    }
                });

                logger.info("mDNS registration done");
                // Wait until the largest possible integer value
                Thread.sleep(Integer.MAX_VALUE);

                /*
                InetAddress[] addressList = InetAddress.getAllByName(args[0]);
                for (InetAddress ia : addressList) {
                    System.out.println(ia.getHostAddress());
                    if (ia.getHostAddress().equalsIgnoreCase(args[1])) {
                        // Create a JmDNS instance
                        JmDNS jmdns = JmDNS.create(ia);

                        // JmDNS jmdns = JmDNS.create( InetAddress.getByName(args[0]));
                        // Register a service
                        ServiceInfo serviceInfo = ServiceInfo.create("_opc._tcp.local.", "_opcua.tcp", 4840, "path=UADiscovery");
                        jmdns.registerService(serviceInfo);

                        System.out.println("Waiting for 500 seconds");
                        // Wait a bit
                        Thread.sleep(500000);

                        // Unregister all services
                        jmdns.unregisterAllServices();

                        break;
                    }
                }non
                 */
            } catch (IOException e) {
                logger.error("jMDNS Error: ", e);
                if (jmdns != null) {
                    jmdns.unregisterAllServices();
                }

            } catch (InterruptedException ex) {
                java.util.logging.Logger.getLogger(MsbUaServer.class.getName()).log(Level.SEVERE, null, ex);
                if (jmdns != null) {
                    jmdns.unregisterAllServices();
                }
            }
        }
    }

    public MsbUaServer() {

    }

    public MsbUaServer(String arg) throws SocketException {

        /*
        if (arg.lastIndexOf("/") != arg.length() - 1) {
            serverName = arg.substring(arg.lastIndexOf("/") + 1);
        } else {
            serverName = arg.substring(0, arg.length() - 1).substring(arg.lastIndexOf("/") + 1);
        }
        if (arg.lastIndexOf("/") != arg.length() - 1) {
            bindingPort = arg.substring(arg.lastIndexOf(":") + 1, arg.lastIndexOf("/"));
        } else {
            bindingPort = arg.substring(0, arg.length() - 1).substring(arg.lastIndexOf(":") + 1, arg.lastIndexOf("/"));
        }
          bindingIP = arg.substring("opc.tcp://".length(), arg.lastIndexOf(":"));
         */
        bindingIP = arg;

        logger.info("MSB Discovery URL: opc.tcp://" + bindingIP + ":" + bindingPort + "/" + serverName);
        //    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    public void startWaitUntilFinish() throws IOException, Exception {

        logger.info("startDiscoveryServer()");


        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
            true, // allow anonymous access
            challenge -> {
                String user0 = "user";
                String pass0 = "password";

                char[] cs = new char[1000];
                Arrays.fill(cs, 'a');
                String user1 = new String(cs);
                String pass1 = new String(cs);

                boolean match0 = user0.equals(challenge.getUsername())
                    && pass0.equals(challenge.getPassword());

                boolean match1 = user1.equals(challenge.getUsername())
                    && pass1.equals(challenge.getPassword());

                return match0 || match1;
            }
        );

        X509IdentityValidator identityValidatorCert = new X509IdentityValidator(x509Certificate -> {
            return true;
        });

        List<UserTokenPolicy> userTokenPolicies = newArrayList(
            OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
            OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME,
            OpcUaServerConfig.USER_TOKEN_POLICY_X509
        );

        // Enforce with security

        X509IdentityProvider x509IdentityProvider = new X509IdentityProvider("openssl_crt.der",
            "herong.key");
        X509Certificate cert = x509IdentityProvider.getCertificate();
        KeyPair keyPair = new KeyPair(cert.getPublicKey(), x509IdentityProvider.getPrivateKey());

        CertificateManager certificateManager = new DefaultCertificateManager(keyPair, cert);
        // No security   
        //  CertificateManager certificateManager = new DefaultCertificateManager();
        File securityDir = new File("./security/");
        if (!securityDir.exists() && !securityDir.mkdirs()) {
            throw new Exception("unable to create security directory");
        }
        CertificateValidator certificateValidator = new DefaultCertificateValidator(securityDir);

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
            .setApplicationName(LocalizedText.english("fortiss opc-ua discovery server"))
            //.setApplicationUri("urn:fortiss:opcua:discovery:server")
            .setApplicationUri("msb.local")
            // .setBindAddresses(newArrayList("localhost"))
            .setBindAddresses(newArrayList(bindingIP))
            .setBindPort(Integer.parseInt(bindingPort))
            .setCertificateManager(certificateManager)
            .setCertificateValidator(certificateValidator)
            .setSecurityPolicies(EnumSet.of(SecurityPolicy.None, SecurityPolicy.Basic256))
            .setProductUri("urn:fortiss:opcua:discovery:sdk")
            .setServerName(serverName)
            .setUserTokenPolicies(userTokenPolicies)
            .setIdentityValidator(identityValidatorCert)
            .build();

        server = new OpcUaServer(serverConfig);

        ConnectToAboveServerExtension cta = new ConnectToAboveServerExtension(server);
        cta.prepareConnectToAbove();

        // Replace the default discovery server by the one supporting server registration
        cdss = new CompleteDiscoveryServiceSet(this, (serverUri, isOnline) -> {
            changeDeviceVisibility(serverUri, isOnline);
            if (!isOnline) {
                heartbeatServer.unregister(serverUri);
            }
        });

        //  server.getServer().addServiceSet(cdss);
        server.getServer().addRequestHandler(RegisterServerRequest.class, cdss::onRegisterServer);
        server.getServer().addRequestHandler(FindServersRequest.class, cdss::onFindAllServers);
        server.getServer().addRequestHandler(GetEndpointsRequest.class, cdss::onGetEndpoints);

        // register a CttNamespace so we have some nodes to play with
        server.getNamespaceManager().registerAndAdd(MsbUaNamespace.NAMESPACE_URI,
            idx -> new MsbUaNamespace(server, idx));

        server.startup();

        Optional<ServerNode> node = server.getNodeMap().getNode(
            new NodeId(server.getNamespaceManager().getNamespaceTable().getIndex(MsbUaNamespace.NAMESPACE_URI),
                "HeartbeatConfig/Port"));

        if (node.isPresent() && node.get() instanceof UaVariableNode) {
            UaVariableNode n = (UaVariableNode) node.get();
            Integer i = (Integer) n.getValue().getValue().getValue();

            heartbeatServer = new HeartbeatServer(i, cdss::unregisterServerAppUri);

            heartbeatServer.start();
        } else {
            throw new Exception("Could not find 'HeartbeatConfig/Port' node in namespace");
        }

        StringBuilder selfInformation = new StringBuilder();

        if (server.getApplicationDescription().getDiscoveryUrls() != null) {
            for (String s : server.getApplicationDescription().getDiscoveryUrls()) {
                selfInformation.append("DiscoveryUrl: ").append(s).append("\n");
            }
        }

        for (EndpointDescription e : server.getServer().getEndpointDescriptions()) {
            selfInformation.append(e.getEndpointUrl()).append("\n");
            selfInformation.append("Application name: ").append(e.getServer().getApplicationName()).append("\n");

            if (e.getServer().getDiscoveryUrls() != null) {
                for (String s : e.getServer().getDiscoveryUrls()) {
                    selfInformation.append("\t").append(s).append("\n");
                }
            }
        }

        logger.info("LDS Info:\n" + selfInformation.toString());

        /*
        KeyStoreLoader loader = new KeyStoreLoader().load();

        SdkServerExample server = new SdkServerExample(loader.getServerCertificate(), loader.getServerKeyPair());
        server.startup();
         */
        // 
        logger.info("Press Ctrl + C to exit");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // print stuff here
            logger.info("\nClosing the MSB controller");
            server.shutdown();
            heartbeatServer.shutdown();
            try {
                heartbeatServer.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
        Thread.sleep(Long.MAX_VALUE); // Sleep "forever"

    }

    public void startup() {
        server.startup();
    }

    public void shutdown() {
        server.shutdown();
    }

    public OpcUaServer getServer() {
        return server;
    }

    public void setServer(OpcUaServer server) {
        this.server = server;
    }

    private void changeDeviceVisibility(String appUri, Boolean isOnline) {
        if (isOnline) {
            if (!uriDeviceIdMapping.containsKey(appUri)) {
                ApplicationDescription ad = cdss.getRegisteredServer(appUri);
                // ad.getDiscoveryUrls() contains the url to be used by the client to get the device id
                // TODO get the id of the device and store it in the uriDeviceIdMapping
            } else {
                // TODO we already know the device, but its nodes are currently hidden. Thus here we need to show them again
            }
        } else {
            // TODO hide the nodes of the device
        }
    }
}
