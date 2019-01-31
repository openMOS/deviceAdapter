package org.fortiss.uaserver.device.lowlevel.skills.atomic;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.fortiss.uaserver.common.MsbGenericComponent;
import org.fortiss.uaserver.device.lowlevel.LboroSkillMethod;
import org.fortiss.uaserver.device.lowlevel.SkillsBackends;
import org.fortiss.uaserver.device.lowlevel.subscriptions.SubscriptionLaserCutting;
import org.fortiss.uaserver.device.lowlevel.subscriptions.SubscriptionLboro;

public class PaintBoardTop extends LboroSkillMethod {

    public PaintBoardTop(String stationName) {
        super();
        skillSubscription = new SubscriptionLaserCutting(stationName);
        this.setSkillSubscription(skillSubscription);
        setAddress(stationName);

    }

    @Override
    public void setMethod(MsbGenericComponent msbComponent, String deviceAdapterId, String recipeId,
            String amlRecipePath, Map<String, String> inputArguments) {
        super.setMethod(msbComponent, deviceAdapterId, recipeId, amlRecipePath, inputArguments);
        if (!inputArguments.isEmpty()) {
            this.programId = inputArguments.get("ProgramID_Parameter/Program");
            this.speed = inputArguments.get("Speed_Parameter/Speed");
        }
    }

    @Override
    public void invokeSkillLowLevelFunctions() throws ExecutionException, InterruptedException {
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
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // while ((Boolean)
        // skillSubscription.readProgramNode(skillSubscription.getNodeHome()).getValue().getValue())

        if (skillSubscription.getNodeReady() == null) {
            throw new ExecutionException(new Throwable("Could not get node ready or node home"));
        } else {
            while (!((String) skillSubscription.readProgramNode(skillSubscription.getNodeReady()).getValue().getValue())
                    .equals("2")) {
            }
        }
        // skillSubscription.writeProgramNode(((SubscriptionJoin)
        // skillSubscription).getNodeRun(), false);
        System.out.println("lboro skill done");
    }

    public void setAddress(String stationName) {
        switch (stationName) {
        case "5":
            this.addr = SkillsBackends.PAINTING_SERVER_1.getAddr();
            break;
        case "6":
            this.addr = SkillsBackends.PAINTING_SERVER_2.getAddr();
            break;
        case "7":
            this.addr = SkillsBackends.PAINTING_SERVER_3.getAddr();
            break;
        default:
            this.addr = SkillsBackends.PAINTING_SERVER_1.getAddr();
        }
    }

}
