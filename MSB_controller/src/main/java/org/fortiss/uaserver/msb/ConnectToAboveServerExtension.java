package org.fortiss.uaserver.msb;

import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.ServerNodeMap;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;

public class ConnectToAboveServerExtension {
	private OpcUaServer server;
	private int NAMESPACE_IDX = 2;

	public ConnectToAboveServerExtension(OpcUaServer server) {
		this.server = server;
	}

	public void prepareConnectToAbove() {
		addFolder(new NodeId(0, 2253), "OPC_Device");
		addObject(new NodeId(2, 5555), "DeviceTopics");
		UInteger[] rank = { UInteger.valueOf(0) };
		Argument in = new Argument("XML_URL", Identifiers.String, ValueRanks.Scalar, rank,
				LocalizedText.english("Server URL"));
		Argument out = new Argument("Result", Identifiers.String, ValueRanks.Scalar, rank,
				LocalizedText.english("OK/NOK"));
		Argument[] argIn = { in };
		Argument[] argOut = { out };
		addSkill(server.getNodeMap(), new NodeId(2, "DeviceTopics"), argIn, argOut, "SendServerUrl");
		in = new Argument("XML_ExecuteOrder", Identifiers.String, ValueRanks.Scalar, rank,
				LocalizedText.english("Send Execute Order"));
		argIn[0] = in;
		addSkill(server.getNodeMap(), new NodeId(2, "DeviceTopics"), argIn, argOut, "SendExecuteOrder");
	}

	private void addFolder(NodeId parentFolder, String name) {
		UaFolderNode folder = new UaFolderNode(server.getNodeMap(), new NodeId(NAMESPACE_IDX, 5555),
				new QualifiedName(NAMESPACE_IDX, name), new LocalizedText("en", name));
		server.getNodeMap().addNode(folder);
		server.getNodeMap().addReference(new Reference(parentFolder, Identifiers.HasComponent,
				folder.getNodeId().expanded(), folder.getNodeClass(), true));
	}

	private void addObject(NodeId parentNode, String name) {
		UaObjectNode internalElement = new UaObjectNode(server.getNodeMap(), new NodeId(NAMESPACE_IDX, name),
				new QualifiedName(NAMESPACE_IDX, name), new LocalizedText("en", name));
		server.getNodeMap().addNode(internalElement);
		server.getNodeMap().addReference(new Reference(parentNode, Identifiers.HasComponent,
				internalElement.getNodeId().expanded(), internalElement.getNodeClass(), true));
	}

	private void addSkill(ServerNodeMap nodeManager, NodeId skillNode, Argument[] inputArguments,
			Argument[] outputArguments, String name) {
		UaMethodNode methodNode = new UaMethodNode(server.getNodeMap(),
				new NodeId(NAMESPACE_IDX, "InvokeSkill/" + name), new QualifiedName(NAMESPACE_IDX, name),
				new LocalizedText(null, name), LocalizedText.english("Triggers the underlying skill of the device."),
				UInteger.valueOf(0), UInteger.valueOf(0), true, true);
		server.getNodeMap().put(new NodeId(NAMESPACE_IDX, "InvokeSkill/" + name), methodNode);
		methodNode.setInputArguments(inputArguments);
		if (outputArguments != null) {
			methodNode.setOutputArguments(outputArguments);
		}
		methodNode.setInvocationHandler(new MethodInvocationHandler() {
			
			@Override
			public void invoke(CallMethodRequest request, CompletableFuture<CallMethodResult> future) {

			}
		});
			
		server.getNodeMap().addReference(new Reference(skillNode, Identifiers.HasComponent,
				methodNode.getNodeId().expanded(), methodNode.getNodeClass(), true));
	}
}
