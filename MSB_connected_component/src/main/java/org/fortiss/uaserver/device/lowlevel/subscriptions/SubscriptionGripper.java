package org.fortiss.uaserver.device.lowlevel.subscriptions;

import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.fortiss.uaserver.device.lowlevel.SubscriptionBasic;

public class SubscriptionGripper extends SubscriptionLboro {


	public SubscriptionGripper(String name) {
		super(SubscriptionGripper.class, name);
		
	}


	@Override
	public NodeId getNodeRun() {
		return nodeRun;
	}

	@Override
	public NodeId getNodeReady() {
		return nodeState;
	}
}
