package org.fortiss.uaserver.device.lowlevel.subscriptions;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.fortiss.uaserver.device.lowlevel.SubscriptionBasic;

public abstract class SubscriptionLboro extends SubscriptionBasic {
    
    protected String stationName;
    protected NodeId methodNode = null;

    public NodeId nodeRun = null;
    public NodeId nodeState = null;
    public NodeId nodeProgramId = null;
    public NodeId nodeSpeed = null;
    

    @Override
    protected void initAfterConnect() {
        Integer nsIdx = 3;
        nodeState = new NodeId(2,
                "/station" + stationName + "/state/data");
        nodeRun = new NodeId(nsIdx,
                "station" + stationName);
        nodeProgramId = new NodeId(nsIdx,
                "|var|CODESYS Control for Raspberry Pi SL.Application.PLC_PRG.SKILL_LASERCUT_PROFILE_ID");
        nodeSpeed = new NodeId(nsIdx,
                "|var|CODESYS Control for Raspberry Pi SL.Application.PLC_PRG.SKILL_LASERCUT_SPEED");
        methodNode = new NodeId(3,2);
    }

	public SubscriptionLboro(Class clazz, String name) {
		super(clazz);
		this.stationName = name;
		// TODO Auto-generated constructor stub
	}

	@Override
	public NodeId getNodeRun() {
		// TODO Auto-generated method stub
		return nodeRun;
	}

	@Override
	public NodeId getNodeReady() {
		// TODO Auto-generated method stub
		return nodeState;
	}

    public NodeId getNodeSpeed() {
        // TODO Auto-generated method stub
        return null;
    }

    public NodeId getNodeProgramId() {
        // TODO Auto-generated method stub
        return null;
    }
	
    public NodeId getNodeKPI() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public NodeId getMethod() {
        // TODO Auto-generated method stub
        return methodNode;
    }
}
