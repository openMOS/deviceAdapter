/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.device.instance;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.model.BasicProperty;
import org.eclipse.milo.opcua.sdk.core.model.Property;
import org.eclipse.milo.opcua.sdk.server.api.ServerNodeMap;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.fortiss.uaserver.device.lowlevel.SkillMethod;

/**
 * 
 * @author cheng
 */
public class Skill {

    private UaObjectNode skillNode;
    private Property<String> skillOperatingMode;
    private Property<String> skillDeviceDescription;
    private SkillMethod method;

    public Skill(ServerNodeMap nodeManager, UShort nameSpaceIndex, NodeId skillTypeNodeId, QualifiedName browseName, LocalizedText displayName, String deviceDescription, SkillMethod md) {

        skillNode = new UaObjectNode.UaObjectNodeBuilder(nodeManager)
                .setNodeId(new NodeId(nameSpaceIndex, "skill"))
                .setBrowseName(browseName)
                .setDisplayName(displayName)
                .setDescription(LocalizedText.english("A motor realizing generic skill type."))
                .setTypeDefinition(skillTypeNodeId)
                .build();

        skillOperatingMode = new BasicProperty<>(
                new QualifiedName(nameSpaceIndex, "OperatingMode"),
                NodeId.parse("ns=0;i=12"),
                -1,
                String.class
        );

        skillDeviceDescription = new BasicProperty<>(
                new QualifiedName(nameSpaceIndex, "DeviceDescription"),
                NodeId.parse("ns=0;i=12"),
                -1,
                String.class
        );

        skillNode.setProperty(skillOperatingMode, "Idle");
        skillNode.setProperty(skillDeviceDescription, deviceDescription);

        this.method = md;

        // Prepare the skill
        UaMethodNode methodNode = new UaMethodNode.UaMethodNodeBuilder(nodeManager)
                .setNodeId(new NodeId(nameSpaceIndex, "InvokeSkill"))
                .setBrowseName(new QualifiedName(nameSpaceIndex, "InvokeSkill"))
                .setDisplayName(new LocalizedText(null, "InvokeSkill"))
                .setDescription(LocalizedText.english("Triggers the underlying skill of the device."))
                .build();

        try {
            AnnotationBasedInvocationHandler invocationHandler
                    = AnnotationBasedInvocationHandler.fromAnnotatedObject(nodeManager, method);

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            nodeManager.put(methodNode.getNodeId(), methodNode);

            skillNode.addReference(new Reference(
                    skillNode.getNodeId(),
                    Identifiers.HasComponent,
                    methodNode.getNodeId().expanded(),
                    methodNode.getNodeClass(),
                    true
            ));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @return the method
     */
    public SkillMethod getMethod() {
        return method;
    }

    /**
     * @param method the method to set
     */
    public void setMethod(SkillMethod method) {
        this.method = method;
    }

    /**
     * @return the skillDeviceDescription
     */
    public Property<String> getSkillDeviceDescription() {
        return skillDeviceDescription;
    }

    /**
     * @param skillDeviceDescription the skillDeviceDescription to set
     */
    public void setSkillDeviceDescription(Property<String> skillDeviceDescription) {
        this.skillDeviceDescription = skillDeviceDescription;
    }

    /**
     * @return the skillNode
     */
    public UaObjectNode getSkillNode() {
        return skillNode;
    }

    /**
     * @param skillNode the skillNode to set
     */
    public void setSkillNode(UaObjectNode skillNode) {
        this.skillNode = skillNode;
    }

    /**
     * @return the skillOperatingMode
     */
    public Property<String> getSkillOperatingMode() {
        return skillOperatingMode;
    }

    /**
     * @param skillOperatingMode the skillOperatingMode to set
     */
    public void setSkillOperatingMode(Property<String> skillOperatingMode) {
        this.skillOperatingMode = skillOperatingMode;
    }

}
