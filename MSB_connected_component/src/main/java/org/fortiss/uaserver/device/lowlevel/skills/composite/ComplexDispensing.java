package org.fortiss.uaserver.device.lowlevel.skills.composite;

import org.fortiss.uaserver.device.lowlevel.CompositeSkillMethod;
import org.fortiss.uaserver.device.lowlevel.SkillsBackends;

public class ComplexDispensing extends CompositeSkillMethod {
    public ComplexDispensing() {
        super();
        setAddress();
    }

    @Override
    public void setAddress() {
        this.addr = SkillsBackends.GLUING_SERVER_1.getAddr(); // to subscribe on energy value
    }

    @Override
    protected void waitForAdapterReady() {
        // TODO Auto-generated method stub
        
    }
}
