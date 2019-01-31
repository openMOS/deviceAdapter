/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.device;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.api.AccessContext;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.fortiss.uaserver.common.MsbGenericComponent;
import org.fortiss.uaserver.common.MsbGenericComponentNamespace;
import org.fortiss.uaserver.device.instance.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sample name space provides a simple calculator, being wrapped as a skill.
 * 
 * @author cheng
 */
public class DeviceUaNamespace extends MsbGenericComponentNamespace {

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public static final String NAMESPACE_URI = "urn:eclipse:milo:opcua:test-namespace";

    Skill skill;

    Timer timer;
    String currentState = "Idle";

    public DeviceUaNamespace(MsbGenericComponent msbComponent, UShort namespaceIndex) {
        super(msbComponent, namespaceIndex, NAMESPACE_URI);


        //addCalculatorSkillNodes(this.getSkillBaseTypeNodeId(), new SqrtSkillMethod(""));
        // addSkillMethodNodes(new SqrtSkillMethod());
        // addStateMachineNodes();

    }
    /*
    private void addStateMachineNodes() {
        FiniteStateMachineNode statusStateMachine = new FiniteStateMachineNode(
                getNodeManager(),
                new NodeId(getNamespaceIndex(), "StateMachine"),
                new QualifiedName(getNamespaceIndex(), "StateMachine"),
                LocalizedText.english("Skill state machine."),
                null,
                null,
                null,
                UByte.valueOf(1)
        );

        getNodeManager().put(statusStateMachine.getNodeId(), statusStateMachine);

        getParentFolder().addReference(new Reference(
                getParentFolder().getNodeId(),
                Identifiers.HasComponent,
                statusStateMachine.getNodeId().expanded(),
                statusStateMachine.getNodeClass(),
                true
        ));
    }
     */

    class PeriodicServiceStatusChangeManager extends TimerTask {

        public void run() {
            if (currentState.equals("Idle")) {
                currentState = "Running";
                skill.getSkillNode().setProperty(skill.getSkillOperatingMode(), "Running");
                logger.info("Skill operating mode from idle to running");
            } else {
                currentState = "Idle";
                skill.getSkillNode().setProperty(skill.getSkillOperatingMode(), "Idle");
                logger.info("Skill operating mode from running to idle");
            }
        }
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
