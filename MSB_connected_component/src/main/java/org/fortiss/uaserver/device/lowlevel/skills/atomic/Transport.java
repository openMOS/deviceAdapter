package org.fortiss.uaserver.device.lowlevel.skills.atomic;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.fortiss.uaserver.common.MsbGenericComponent;
import org.fortiss.uaserver.device.lowlevel.LboroSkillMethod;
import org.fortiss.uaserver.device.lowlevel.SkillsBackends;
import org.fortiss.uaserver.device.lowlevel.SubscriptionBasic;
import org.fortiss.uaserver.device.lowlevel.subscriptions.SubscriptionLboro;
import org.fortiss.uaserver.device.lowlevel.subscriptions.SubscriptionTransport;

public class Transport extends LboroSkillMethod {

    public Transport() {
        super();
        skillSubscription = new SubscriptionTransport("");
        this.setSkillSubscription(skillSubscription);
        setAddress();
    }
    

    @Override
    public void setMethod(MsbGenericComponent msbComponent, String deviceAdapterId, String recipeId,
            String amlRecipePath, Map<String, String> inputArguments) {
        super.setMethod(msbComponent, deviceAdapterId, recipeId, amlRecipePath, inputArguments);
        if (!inputArguments.isEmpty()) {
            this.programId = inputArguments.get("Route_Parameter/Program");
            this.speed = inputArguments.get("Velocity_Parameter/Velocity");
        }
    }
    
    @Override
    public void invokeSkillLowLevelFunctions() throws ExecutionException, InterruptedException{
        SubscriptionLboro skillSubscription = (SubscriptionLboro) this.getSubscription();

        logger.info("invokeSkillLowLevelFunctions()");
        try {
            skillSubscription.startConnection(addr);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // skillSubscription.writeProgramNode(((SubscriptionLboro)
        // skillSubscription).getNodeProgramId(),
        // new Variant(Short.valueOf(programId)));
        // skillSubscription.writeProgramNode(((SubscriptionLboro)
        // skillSubscription).getNodeSpeed(),
        // Float.parseFloat(speed));
        // skillSubscription.writeProgramNode(((SubscriptionLboro)
        // skillSubscription).getNodeRun(), true);

        Variant[] input = new Variant[2];

        input[0] = new Variant(Short.valueOf(programId));
        input[1] = new Variant(Float.parseFloat(speed));

        skillSubscription.callMethod(((SubscriptionLboro) skillSubscription).getNodeRun(),
                ((SubscriptionLboro) skillSubscription).getMethod(), input);
        

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // while ((Boolean)
        // skillSubscription.readProgramNode(skillSubscription.getNodeHome()).getValue().getValue())

        if (skillSubscription.getNodeReady() == null) {
            throw new ExecutionException(new Throwable("Could not get node ready or node home"));
           
        } else {
            while (!((String) skillSubscription.readProgramNode(skillSubscription.getNodeReady()).getValue().getValue()).replaceAll("\\\\x0", " ").split(" ")[Short.valueOf(programId)]
                    .equals("0")) {
            }
        }
        // skillSubscription.writeProgramNode(((SubscriptionJoin)
        // skillSubscription).getNodeRun(), false);
        System.out.println("lboro skill done");
    }

    public void setAddress() {
        
            this.addr = SkillsBackends.AGV_SERVER.getAddr();
    }
}
