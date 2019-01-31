package org.fortiss.uaserver.device.lowlevel.skills.composite;

import org.fortiss.uaserver.device.lowlevel.CompositeSkillMethod;
import org.fortiss.uaserver.device.lowlevel.SkillsBackends;

public class TaskFull extends CompositeSkillMethod {

	public TaskFull(Boolean ford) {
		super(ford);
		setAddress();
	}

	@Override
	public void setAddress() {
		if (ford) {
		//	this.addr = SkillsBackends.FORD_POWER_SERVER.getAddr();
		} else {
			this.addr = SkillsBackends.GLUING_SERVER.getAddr(); // to subscribe on energy value
		}
	}
}
