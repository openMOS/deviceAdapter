package org.fortiss.uaserver.device.instance;

import java.util.List;

import org.fortiss.uaserver.device.lowlevel.SkillMethod;

public abstract class Module {

	private List<SkillMethod> skills;
	
	public void setSkills (List<SkillMethod> skills) {
		this.skills = skills;
	}

}
