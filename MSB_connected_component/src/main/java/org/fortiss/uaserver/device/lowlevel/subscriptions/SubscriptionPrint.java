package org.fortiss.uaserver.device.lowlevel.subscriptions;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class SubscriptionPrint extends SubscriptionLboro{

    public SubscriptionPrint(String name) {
        super(SubscriptionPrint.class, name);

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

}
