package org.fortiss.masmec;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.stack.core.application.CertificateManager;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.UserTokenPolicy;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Lists.newArrayList;

public class MasmecDummy {


    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MasmecDummy.class);
    private OpcUaServer server;


    private URL fullEndpointUrl;
    private String serverName;

    public static void main(String[] args) {

        if (args.length != 1) {
            System.err.println("Usage: listenEndpointURL");
            return;
        }

        MasmecDummy s = new MasmecDummy(args[0]);
        try {
            s.start();

            logger.info("==== Dummy Initialized. Ready to be ruled by Device Adapter ====");

            s.waitUntilFinish();
        } catch (Exception ex) {
            logger.error("Could not initialize device!", ex);
        }
    }

    private MasmecDummy(String endpointUrl) {

        URL.setURLStreamHandlerFactory(protocol -> "opc.tcp".equals(protocol) ? new URLStreamHandler() {
            protected URLConnection openConnection(URL url) throws IOException {
                return new URLConnection(url) {
                    public void connect() throws IOException {
                        // do nothing
                    }
                };
            }
        } : null);

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
            serverName = "";
        }
        logger.info("Binding server on: " + fullEndpointUrl.toString());
    }

    private void start() throws Exception {

        logger.info("startServer()");

        OpcUaServerConfig serverConfig = getServerConfig();

        server = new OpcUaServer(serverConfig);

        // register a CttNamespace so we have some nodes to play with
        server.getNamespaceManager().registerAndAdd("S7:",
            idx -> new MasmecDummyNamespace(server, idx, "S7:"));

        server.startup();
    }

    protected OpcUaServerConfig getServerConfig() throws Exception {
        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(true, challenge -> true);

        List<UserTokenPolicy> userTokenPolicies = newArrayList(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
            OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME);

        CertificateManager certificateManager = new DefaultCertificateManager();
        File securityDir = new File("./security/");
        if (!securityDir.exists() && !securityDir.mkdirs()) {
            throw new Exception("unable to create security directory");
        }
        CertificateValidator certificateValidator = new DefaultCertificateValidator(securityDir);

        return OpcUaServerConfig.builder().setApplicationName(new LocalizedText("", "S7 Dummy"))
            .setApplicationUri("urn::s7").setBindAddresses(newArrayList(fullEndpointUrl.getHost()))
            .setBindPort(fullEndpointUrl.getPort()).setCertificateManager(certificateManager)
            .setCertificateValidator(certificateValidator)
            .setSecurityPolicies(EnumSet.of(SecurityPolicy.None, SecurityPolicy.Basic128Rsa15))
            .setProductUri("urn:s7:dummy").setServerName(serverName).setUserTokenPolicies(userTokenPolicies)
            .setIdentityValidator(identityValidator).build();
    }

    public void waitUntilFinish() throws IOException {
        logger.info("Press any key to exit");
        System.in.read();
    }
}
