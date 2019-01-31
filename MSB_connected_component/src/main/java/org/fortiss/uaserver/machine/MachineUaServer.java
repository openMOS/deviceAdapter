/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.machine;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.fortiss.uaserver.common.MsbGenericComponent;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MachineUaServer extends MsbGenericComponent {

	private OpcUaServer server;

	private ConcurrentHashMap<String, OpcUaClient> skillUaClientMap = new ConcurrentHashMap<>();

	public static void main(String[] args) {
		setLoggerFile(args[0]);

		MachineUaServer s = new MachineUaServer(args[0]);
		try {
			s.start();

			final CompletableFuture<OpcUaClient> promiseDiscoveryEndpointClient = new CompletableFuture<>();
			s.startupMdns("Java Machine", promiseDiscoveryEndpointClient);

			promiseDiscoveryEndpointClient.thenAccept(opcUaClient -> {
				if (s.register(opcUaClient)) {


					java.util.logging.Logger.getLogger(MachineUaServer.class.getName()).info("Sucessfully registered with LDS. Getting MSB endpoint URL");
					if (!s.initMsbEndpoint(args[1], "urn:MSB:opcua:server")) {
						System.exit(1);
					}

				}
			});
			s.waitUntilFinish();

			s.stopHeartbeat();
		} catch (Exception ex) {
			java.util.logging.Logger.getLogger(MachineUaServer.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	@Override
	protected String getApplicationUri() {
		return "urn:machine:opcua:server";
	}

	@Override
	protected String getProductUri() {
		return "urn:machine:opcua:sdk";
	}

	@Override
	protected LocalizedText getApplicationName() {
		return LocalizedText.english("machine opc-ua server");
	}

	private MachineUaServer(String arg) {
		super(arg);
	}

	private void start() throws IOException, Exception {

		logger.info("startServer()");

		OpcUaServerConfig serverConfig = getServerConfig();

		server = new OpcUaServer(serverConfig);

		// register a CttNamespace so we have some nodes to play with
		server.getNamespaceManager().registerAndAdd(MachineUaNamespace.NAMESPACE_URI,
				idx -> new MachineUaNamespace(this, idx));

		server.startup();

        /*
		KeyStoreLoader loader = new KeyStoreLoader().load();

        MachineUaServer server = new MachineUaServer(loader.getServerCertificate(), loader.getServerKeyPair());
        server.startupMdns();
         */
		//
	}

	@Override
	public OpcUaServer getServer() {
		return server;
	}

	/**
	 * @return the skillUaClientMap
	 */
	public ConcurrentHashMap<String, OpcUaClient> getSkillUaClientMap() {
		return skillUaClientMap;
	}

	/**
	 * @param aSkillUaClientMap the skillUaClientMap to set
	 */
	public void setSkillUaClientMap(ConcurrentHashMap<String, OpcUaClient> aSkillUaClientMap) {
		skillUaClientMap = aSkillUaClientMap;
	}
}
