/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.machine;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.fortiss.uaserver.common.MsbGenericComponentNamespace;

public class MachineUaNamespace extends MsbGenericComponentNamespace {

	public static final String NAMESPACE_URI = "urn:openmos:skills-namespace";

	private MachineUaServer machineServer;

	public MachineUaNamespace(MachineUaServer server, UShort namespaceIndex) {
		super(server, namespaceIndex, NAMESPACE_URI);
		this.machineServer = server;

		UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeMap())
				.setNodeId(new NodeId(getNamespaceIndex(), "ServiceLevel"))
				.setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
				.setBrowseName(new QualifiedName(getNamespaceIndex(), "ServiceLevel"))
				.setDisplayName(LocalizedText.english("ServiceLevel")).setDataType(Identifiers.String)
				.setTypeDefinition(Identifiers.BaseDataVariableType).build();

		getSkillTypeNode().addProperty(node);
	}

	public MachineUaServer getMachineServer() {
		return machineServer;
	}

	@Override
	public CompletableFuture<List<Reference>> browse(AccessContext context, NodeId nodeId) {
		ServerNode node = this.getNodeMap().get(nodeId);

		if (node != null) {
			return CompletableFuture.completedFuture(node.getReferences());
		} else {
			CompletableFuture<List<Reference>> f = new CompletableFuture<>();
			f.completeExceptionally(new UaException(StatusCodes.Bad_NodeIdUnknown));
			return f;
		}
	}
}
