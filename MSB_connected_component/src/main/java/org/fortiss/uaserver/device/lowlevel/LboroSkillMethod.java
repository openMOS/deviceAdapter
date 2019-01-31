package org.fortiss.uaserver.device.lowlevel;

import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.fortiss.uaserver.device.instance.SkillState;
import org.fortiss.uaserver.device.lowlevel.subscriptions.SubscriptionLboro;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LboroSkillMethod extends SkillMethod{

    protected String speed;
    protected String programId;
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected SubscriptionLboro skillSubscription;
    // private SubscriptionKPIs kpiSubscription;



    @Override
    public void setAddress() {
        // TODO Auto-generated method stub
        
    }
    
    public abstract void invokeSkillLowLevelFunctions() throws ExecutionException, InterruptedException;

    @Override
    protected void invokeDetached(InvocationContext context, String prdId, String prdType, Boolean searchForNextRecipe, String sr_id)
            throws Exception {

        invokeSkillLowLevelFunctions();
        
    }
    



    @Override
    protected void waitForAdapterReady() {
        if (skillSubscription.getNodeReady() == null) {
            try {
                throw new ExecutionException(new Throwable("Could not get node ready or node home"));
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            try {
                while (!((String) skillSubscription.readProgramNode(skillSubscription.getNodeReady()).getValue().getValue())
                        .equals("0")) {
                }
                deviceAdapterStateNode.setValue(new DataValue(new Variant(SkillState.Ready.toString())));
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
