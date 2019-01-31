/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.device.lowlevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.fortiss.uaserver.common.MsbGenericComponent;
import org.fortiss.uaserver.device.instance.AMLParser;
import org.fortiss.uaserver.device.instance.SkillState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SkillMethod {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected MsbGenericComponent msbComponent;
    protected String deviceAdapterId;
    protected String productId;
    protected String productType;
    protected String recipeId;
    protected String skillRequirementId;
    protected String addr;
    protected SubscriptionBasic subscription;
    protected Boolean searchForNextRecipe;
    // private String deviceAdapterName;
    // private String recipeName;
    protected Map<String, String> inputArguments;
    protected UaVariableNode deviceAdapterStateNode;

    // Note the number of threads should be at least the number of possible active
    // skills at the same time
    protected static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    private List<String> dependendSkillNeedsReady = new ArrayList<String>();
    protected String amlRecipePath;

    protected SkillState skillState;

    protected NodeId informationPortChild;

    public SkillMethod() {

    };

    public abstract void setAddress();

    public SubscriptionBasic getSubscription() {
        return this.subscription;
    }

    public void setSkillSubscription(SubscriptionBasic subscription) {
        this.subscription = subscription;
    }

    public void setMethod(MsbGenericComponent msbComponent, String deviceAdapterId, String recipeId,
            String amlRecipePath, Map<String, String> inputArguments) {
        this.msbComponent = msbComponent;
        this.deviceAdapterId = deviceAdapterId;
        this.amlRecipePath = amlRecipePath;
        this.recipeId = recipeId;
        this.inputArguments = inputArguments;
        String tmp = amlRecipePath.substring(0, amlRecipePath.lastIndexOf("/"));
        deviceAdapterStateNode =

                getServerNode("DeviceAdapterState/" + tmp.substring(tmp.lastIndexOf("/") + 1));
        deviceAdapterStateNode.setValue(new DataValue(new Variant(SkillState.Ready.toString())));
    }

    public MsbGenericComponent getMsbComponent() {
        return msbComponent;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void updateInputArguments(Map<String, String> newArguments) {
        this.inputArguments = newArguments;
    }

    public void initialize() {
        this.informationPortChild = getInformationPortChild(new NodeId(2, amlRecipePath));
        if (informationPortChild == null) {
            logger.warn("Recipe has no information port child. Setting KPI is not supported.");
        }
        setKpiExecutionTime(0, true);
        setSkillState(SkillState.Ready, true);
    }

    public void addDependendReadySkill(String skillName) {
        this.dependendSkillNeedsReady.add(skillName);
    }

    private UaVariableNode getServerNode(String nodeId) {
        Optional<ServerNode> serverNode = msbComponent.getServer().getNodeMap()
                .getNode(new NodeId(AMLParser.NAMESPACE_IDX, nodeId));
        if (!serverNode.isPresent()) {
            logger.warn("Not setting node value. Node '{}:{}' is missing", AMLParser.NAMESPACE_IDX, nodeId);
            return null;
        }
        ServerNode node = serverNode.get();
        if (!(node instanceof UaVariableNode)) {
            logger.warn("Not setting node value. Node '{}:{}' is not a variable node", AMLParser.NAMESPACE_IDX, nodeId);
            return null;
        }
        return (UaVariableNode) node;
    }

    public void setSkillState(SkillState newState, boolean silent) {
        UaVariableNode node = getServerNode(
                "SkillState/" + amlRecipePath.substring(amlRecipePath.lastIndexOf("/") + 1));
        if (node == null) {
            return;
        }
        this.skillState = newState;
        node.setValue(new DataValue(new Variant(this.skillState.toString())));
        if (!silent)
            logger.info("Set SkillState to {}", this.skillState.toString());

        if (deviceAdapterStateNode == null) {
            return;
        }
        this.skillState = newState;
        if (newState.equals(SkillState.Running)) {
            deviceAdapterStateNode.setValue(new DataValue(new Variant(newState.toString())));
            if (((String)deviceAdapterStateNode.getNodeId().getIdentifier()).contains("AGV")){
                deviceAdapterStateNode.setValue(new DataValue(new Variant(SkillState.Ready.toString())));
            }
        }
        if (!silent)
            logger.info("Set SkillState to {}", this.skillState.toString());

    }

    public SkillState getSkillState() {
        return this.skillState;
    }

    public void setKpi(Variant value, String kpi) {
        if (informationPortChild == null) {
            logger.warn("Cannot set KPI. InfoPort is missing");
            return;
        }

        String fullNodeId = informationPortChild.getIdentifier().toString() + "/" + kpi;
        UaVariableNode node = getServerNode(fullNodeId);
        if (node == null) {
            logger.warn("Cannot set KPI. Node missing {}", fullNodeId);
            return;
        }

        node.setValue(new DataValue(value));
        logger.debug("Set KPI {} to {} at {}", kpi, value, fullNodeId);
    }

    protected NodeId getInformationPortChild(NodeId parentNode) {
        Optional<ServerNode> parentOptional = msbComponent.getServer().getNodeMap().getNode(parentNode);
        if (!parentOptional.isPresent()) {
            return null;
        }
        ServerNode parent = parentOptional.get();
        for (Reference r : parent.getReferences()) {
            if (r.getReferenceTypeId().equals(Identifiers.HasComponent)) {
                Optional<ServerNode> subcomponent = msbComponent.getServer().getNodeMap()
                        .getNode((r.getTargetNodeId()));
                if (!subcomponent.isPresent())
                    continue;
                for (Reference rr : subcomponent.get().getReferences()) {
                    // 4001 == HasAMLRoleReference
                    if (rr.getReferenceTypeId().equals(new NodeId(1, 4001))
                            && rr.getTargetNodeId().getIdentifier().equals("openMOSRoleClassLib/InformationPort")) {
                        return rr.getSourceNodeId();
                    }
                }
            }
        }
        return null;
    }

    protected void setKpiExecutionTime(int executionTime, boolean silent) {
        if (executionTime < 0) {
            logger.error("Execution time is negative. Setting it to 0");
            executionTime = 0;
        }
        if (!silent) {
            logger.info("[KPI] Execution time: {}ms", executionTime);
        }
        setKpi(new Variant(UInteger.valueOf(executionTime)), "DurationKPI/value");

    }

    public void setKpiAirConsumption(double value) {
        logger.info("[KPI] Air Consumption: {}", value);
        setKpi(new Variant(value), "AirConsumptionKPI/value");
    }

    public void setKpiAGVConsumption(double value) {
        logger.info("[KPI] Energy Consumption: {}", value);
        setKpi(new Variant(value), "ConsumptionKPI/value");
    }

    public void setKpiRobotEnergyConsumption(double value) {
        logger.info("[KPI] Robot Consumption: {}", value);
        setKpi(new Variant(value), "Robot-EnergyConsumptionKPI/value");
    }

    public void setKpiCellEnergyConsumption(double value) {
        logger.info("[KPI] Cell Consumption: {}", value);
        setKpi(new Variant(value), "Cell-EnergyConsumptionKPI/value");
    }

    public String getFullNodeId(Boolean ford) {

        if (ford) {
            return "Pre-Demonstrator_InstanceHierarchy/AssemblySystem/WorkStation_Ford/SC2: TaskFull_Recipe/InformationPort/Vis-InspectKPI/value";
        }
        return "Introsys_Demonstrator_InstanceHierarchy/AssemblySystem/WorkStation_VW/SC2: VW_TaskFull_Recipe/InformationPort/Vis-InspectKPI/value";
    }

    public void setKpiInspectionScore(double value, boolean ford) {
        logger.info("[KPI] Inspection score: {}", value);
        try {
            String fullNodeId = getFullNodeId(ford);
            UaVariableNode node = getServerNode(fullNodeId);
            if (node == null) {
                logger.warn("Cannot set KPI. Node missing {}", fullNodeId);
                return;
            }
            node.setValue(new DataValue(new Variant(Double.toString(value))));
        } catch (NullPointerException e) {

        }
    }

    public void setKpiVelocity(double value) {
        logger.info("[KPI] Velocity: {}", value);
        setKpi(new Variant(value), "VelocityKPI/value");
    }

    void assertSkillReady() throws UaException {
        if (skillState == SkillState.Running) {
            logger.warn("invoke called while recipe is in execution state. Returning Bad_InvalidState");
            throw new UaException(StatusCodes.Bad_InvalidState, "The recipe is currently being executed");
        }
        for (String skill : dependendSkillNeedsReady) {
            UaVariableNode node = getServerNode("SkillState/" + skill);
            if (node == null) {
                logger.error("Ignoring state of skill '{}'. Its state is not found in the local server.", skill);
                continue;
            }
            String state = (String) node.getValue().getValue().getValue();
            if (state.equals(SkillState.Running.toString())) {
                logger.warn("invoke called while dependent recipe {} is running. Returning Bad_InvalidState", skill);
                throw new UaException(StatusCodes.Bad_InvalidState, "The recipe is currently being executed");
            }
        }
    }

    @UaMethod
    public void invoke(AnnotationBasedInvocationHandler.InvocationContext context,
            @UaInputArgument(name = "product_id", description = "Product id") String prdId,
            @UaInputArgument(name = "product_type", description = "Product type") String prdType,
            @UaInputArgument(name = "search_for_next_recipe", description = "Search for next recipe") Boolean searchForNextRecipe,
            @UaInputArgument(name = "skill_requirement_id", description = "SkillRequirementId") String skillRequirementId)
            throws UaException {
        this.skillRequirementId = skillRequirementId;
        assertSkillReady();
        setSkillState(SkillState.Running, true);
        long startTime = System.currentTimeMillis();
        setProductType(prdType);
        setSearchForNextRecipe(searchForNextRecipe);
        setProductId(prdId);
        logger.info("invoke Skill({})", prdId);
        scheduler.schedule(() -> {
            setKpiExecutionTime(0, true);
            boolean success;
            try {
                invokeDetached(context, prdId, prdType, searchForNextRecipe, skillRequirementId);
                logger.info("invoke done");
                success = true;
                setSkillState(SkillState.Ready, true);
            } catch (Exception e) {
                logger.error("Invoke failed", e);
                success = false;
                setSkillState(SkillState.Error, true);
            }
            long endTime = System.currentTimeMillis();
            setKpiExecutionTime((int) (endTime - startTime), false);
            respondMSB(success);
            waitForAdapterReady();
        }, 0, TimeUnit.SECONDS);
    }

    protected abstract void invokeDetached(AnnotationBasedInvocationHandler.InvocationContext context, String prdId,
            String prdType, Boolean searchForNextRecipe, String sr_id) throws Exception;

    protected abstract void waitForAdapterReady();

    void respondMSB(boolean isSuccess) {
        if (!isSuccess) {
            // TODO set the skill state to error state and then call the changeState
            // for now we skip the changeState to avoid continuing the process
            return;
        }
        CompletableFuture<CallMethodResult> res = msbComponent.changeState(deviceAdapterId, recipeId, productId,
                productType, searchForNextRecipe, "Ready", skillRequirementId);// recipeId
        // for
        try {
            CallMethodResult result = res.get();
            if (result.getStatusCode().isGood())
                logger.info("Change State call SUCCESS");
            else
                logger.error("Change state call ERROR: " + res.get());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Change State call failed.", e);
        }
    }

    public void invokeSkillLowLevelFunctionsBase(SubscriptionBasic subscription, String skillName) throws Exception {
        logger.info("invokeSkillLowLevelFunctions()");

        if (subscription.getNodeReady() != null) {
            logger.info("Resetting ready node");
            subscription.writeProgramNode(subscription.getNodeReady(), false);
        }

        NodeId nodeRun = subscription.getNodeRun();
        boolean stopRun = false;
        if (nodeRun != null && nodeRun.isNotNull()) {
            logger.debug("Write node skill " + skillName + "run");
            subscription.writeProgramNode(nodeRun, true);
            stopRun = true;
            logger.info("Write nodeRun triggered for " + skillName);
        } else {
            logger.debug("nodeRun is NULL. Skipping");
        }

        waitForTrueInBooleanNode(subscription, subscription.getNodeReady());
        if (stopRun) {
            logger.info("Setting nodeRun to false");
            subscription.writeProgramNode(nodeRun, false);
        }
    }

    public void setProductId(String prdId) {
        productId = prdId;
    }

    public void setSearchForNextRecipe(Boolean next) {
        this.searchForNextRecipe = next;
    }

    public void setProductType(String prodType) {
        this.productType = prodType;
    }

    public boolean waitForTrueInBooleanNode(SubscriptionBasic subscription, NodeId nodeId) {
        if (nodeId == null || nodeId.isNull())
            return false;

        logger.info("Waiting for true in {}", nodeId.getIdentifier().toString());
        Boolean val = false;
        try {
            while (!val) {
                val = (Boolean) subscription.readProgramNode(nodeId).getValue().getValue();
            }
            logger.info("Got node {}", nodeId);
            subscription.writeProgramNode(nodeId, false);
            logger.info("Tried to reset {}", nodeId);
            return true;
        } catch (Exception e) {
            logger.error("Could not wait for READ.", e);
        }
        return false;
    }

    protected boolean waitForIntegerNode(SubscriptionBasic subscription, NodeId nodeId, Integer resDone) {
        if (nodeId == null || nodeId.isNull())
            return false;

        logger.info("Waiting for not 0 in {}", nodeId.getIdentifier().toString());
        int val = 0;
        try {
            while (val != resDone) {
                val = (Integer) subscription.readProgramNode(nodeId).getValue().getValue();
            }
            logger.info("Got node {}", nodeId);
            subscription.writeProgramNode_AGV(nodeId, (Integer) 0);
            logger.info("Tried to reset {}", nodeId);
            return true;
        } catch (Exception e) {
            logger.error("Could not wait for READ.", e);
        }
        return false;
    }
}
