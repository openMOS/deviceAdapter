package org.fortiss.uaserver.device.lowlevel.subscriptions;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.fortiss.uaserver.device.lowlevel.SubscriptionBasic;

public class SubscriptionJoin extends SubscriptionLboro {

    public SubscriptionJoin(String name) {
        super(SubscriptionJoin.class, name);

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
