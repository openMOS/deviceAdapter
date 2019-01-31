/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.fortiss.uaserver.device;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.RegisterServerResponse;
import org.fortiss.uaserver.common.MsbGenericComponent;
import org.slf4j.LoggerFactory;

public class DeviceUaServer extends MsbGenericComponent {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(DeviceUaServer.class);

    // static String serverName = "test-device-server";

    private OpcUaServer server;
    public static String amlFile;

    /**
     * Create a map which maps each module with the corresponding OPC UA client
     */
    public static HashMap<String, OpcUaClient> moduleLowLevelControlMap;

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("Usage: listenEndpointURL discoveryServerEndpointURL amlFileName");
            return;
        }

        setLoggerFile(args[0]);

        amlFile = args[2];

        DeviceUaServer s = new DeviceUaServer(args[0]);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            s.getLogger().warn("Shutting down device");
            s.stop();
        }));

        try {
            s.start();

            s.getLogger().info("Trying to register with LDS at " + args[1]);

            OpcUaClient ldsEndpointClient =
                MsbGenericComponent.getClientForSuitableEndpoint(args[1],
                    s::getRegisterClientConfig,
                    null, null, null, s.getLogger()).get();

            if (!s.register(ldsEndpointClient)) {
                return;
            }

            s.getLogger().info("Sucessfully registered with LDS. Getting MSB endpoint URL");
            if (!s.initMsbEndpoint(args[1], "urn:MSB:opcua:server")) {
                return;
            }

            s.getLogger().info("==== Device Initialized. Ready to be ruled by MSB ====");

            s.waitUntilFinish();

            s.stop();
        } catch (Exception ex) {
            s.getLogger().error("Could not initialize device!", ex);
        }

    }

    @Override
    protected String getApplicationUri() {
        return "urn:" + getServerName() + ":opcua:server";
    }

    @Override
    protected String getProductUri() {
        return "urn:" + getServerName() + ":opcua:sdk";
    }

    @Override
    protected LocalizedText getApplicationName() {
        return LocalizedText.english(getServerName());
    }

    @Override
    public OpcUaServer getServer() {
        return server;
    }

    public DeviceUaServer(String endpointUrl) {
        super(endpointUrl);
        moduleLowLevelControlMap = new HashMap<>();

    }

    private org.slf4j.Logger getLogger() {
        return logger;
    }

    private void start() throws Exception {

        logger.info("startServer()");

        OpcUaServerConfig serverConfig = getServerConfig();

        server = new OpcUaServer(serverConfig);

        // register a CttNamespace so we have some nodes to play with
        server.getNamespaceManager().registerAndAdd(DeviceUaNamespace.NAMESPACE_URI,
            idx -> new DeviceUaNamespace(this, idx));

        server.startup();
    }

    private void stop() {
        this.stopWaitLoop();

        RegisterServerResponse response;
        try {
            response = this.unregister().get();
            if (response.getResponseHeader().getServiceResult().isGood()) {
                logger.info("Successfully unregistered from LDS");
            } else {
                logger.warn("Could not unregister from discovery server. Status = {}",
                    response.getResponseHeader().getServiceResult().toString());
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could not unregister from discovery server: ", e);
        }
        server.shutdown();
    }

}
