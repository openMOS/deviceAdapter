package org.fortiss.uaserver.device.lowlevel.subscriptions;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class SubscriptionGluing extends SubscriptionLboro {
    

    public SubscriptionGluing(String name) {
        super(SubscriptionGluing.class, name);

    }
    
    @Override
    protected void initAfterConnect() {
        Integer nsIdx = 3;
        nodeState = new NodeId(2,
                "/station9/state/data");
        nodeRun = new NodeId(nsIdx,
                "station9");
        nodeProgramId = new NodeId(nsIdx,
                "|var|CODESYS Control for Raspberry Pi SL.Application.PLC_PRG.SKILL_LASERCUT_PROFILE_ID");
        nodeSpeed = new NodeId(nsIdx,
                "|var|CODESYS Control for Raspberry Pi SL.Application.PLC_PRG.SKILL_LASERCUT_SPEED");
        methodNode = new NodeId(3,5);
    }

    @Override
    public NodeId getNodeRun() {
        return nodeRun;
    }

    @Override
    public NodeId getNodeReady() {
        return nodeState;
    }

    public NodeId getNodeProgramId() {
        return nodeProgramId;
    }

    public NodeId getNodeSpeed() {
        return nodeSpeed;
    }
    
    public NodeId getMethod() {
        return methodNode;
    }
}