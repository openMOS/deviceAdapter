package org.fortiss.uaserver.device.lowlevel.subscriptions;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

public class SubscriptionTransport extends SubscriptionLboro {
    public SubscriptionTransport(String name) {
        super(SubscriptionJoin.class, name);

    }

    @Override
    protected void initAfterConnect() {
        Integer nsIdx = 3;
        nodeState = new NodeId(2, "/AGV/availability/data");
        nodeRun = new NodeId(3, "AGV");
        nodeProgramId = new NodeId(nsIdx,
                "|var|CODESYS Control for Raspberry Pi SL.Application.PLC_PRG.SKILL_LASERCUT_PROFILE_ID");
        nodeSpeed = new NodeId(nsIdx,
                "|var|CODESYS Control for Raspberry Pi SL.Application.PLC_PRG.SKILL_LASERCUT_SPEED");
        methodNode = new NodeId(3, 2);
        nodeState = new NodeId(2, "/AGV/routeActivity/data");
    }
}
