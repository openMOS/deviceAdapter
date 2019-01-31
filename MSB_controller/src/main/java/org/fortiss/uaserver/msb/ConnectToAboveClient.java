/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.msb;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.Arrays;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

public class ConnectToAboveClient {

	//private final Logger logger = LoggerFactory.getLogger(getClass());

	OpcUaClient client;

	public ConnectToAboveClient(String s) {
		// endpoint to connect << output of SendServerUrl Skill
		String connectedServerDiscoveryPoint = s;
		try {
			EndpointDescription[] endpoints = UaTcpStackClient.getEndpoints(connectedServerDiscoveryPoint).get();

			EndpointDescription endpoint = Arrays.stream(endpoints)
					.filter(e -> e.getSecurityPolicyUri().equals(SecurityPolicy.None.getSecurityPolicyUri()))
					.findFirst().orElseThrow(() -> new Exception("no desired endpoints returned"));

			OpcUaClientConfig clientConfig = OpcUaClientConfig.builder()
					.setApplicationName(LocalizedText.english("fortiss opc-ua client"))
					.setApplicationUri("urn:fortiss:opcua:client").setEndpoint(endpoint).setRequestTimeout(uint(60000))
					.build();

			client = new OpcUaClient(clientConfig);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void run(){
		client.connect();
	}
}
