/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.common.type;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.api.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.ServerNodeMap;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cheng
 */
public class SkillTypeNode extends UaObjectTypeNode {

    UaVariableNode operatingModeNode;
    UaVariableNode deviceDescriptionNode;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SkillTypeNode(ServerNodeMap nodeManager, NodeId nodeId, QualifiedName browseName, LocalizedText displayName,
            LocalizedText description, UInteger writeMask, UInteger userWriteMask, boolean isAbstract, UShort namespaceIndex) {

        super(nodeManager, nodeId, browseName, displayName, description, writeMask, userWriteMask, isAbstract);

        operatingModeNode = new UaVariableNode.UaVariableNodeBuilder(getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "OperatingMode"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "operatingMode"))
                .setDisplayName(LocalizedText.english("OperatingMode"))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        deviceDescriptionNode = new UaVariableNode.UaVariableNodeBuilder(getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "DeviceDescription"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "DeviceDescription"))
                .setDisplayName(LocalizedText.english("DeviceDescription"))
                .setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        this.addProperty(operatingModeNode);
        this.addProperty(deviceDescriptionNode);

        // Add a member function to the specified type (failed to work)
        UaMethodNode methodNode = new UaMethodNode.UaMethodNodeBuilder(getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "InvokeSkill"))
                .setBrowseName(new QualifiedName(namespaceIndex, "InvokeSkill"))
                .setDisplayName(new LocalizedText(null, "InvokeSkill"))
                .setDescription(LocalizedText.english("Triggers the underlying skill of the device."))
                .build();


        // ValueRank -2 = Any
        Argument input[] =  {new Argument("input", new NodeId(0,"BaseDataType"),-2, null, new LocalizedText("en", "Base Input"))};
        Argument output[] = {new Argument("output", new NodeId(0,"BaseDataType"),-2, null, new LocalizedText("en", "Base Output"))};

        methodNode.setProperty(UaMethodNode.InputArguments, input);
        methodNode.setProperty(UaMethodNode.OutputArguments, output);
        methodNode.setInvocationHandler(new MethodInvocationHandler() {
            @Override
            public void invoke(CallMethodRequest callMethodRequest, CompletableFuture<CallMethodResult> completableFuture) {
                logger.warn("invokeSkill not overwritten in object instance");
            }
        });

        getNodeMap().put(methodNode.getNodeId(), methodNode);

        this.addReference(new Reference(
                this.getNodeId(),
                Identifiers.HasComponent,
                methodNode.getNodeId().expanded(),
                methodNode.getNodeClass(),
                true
        ));
    }

}
