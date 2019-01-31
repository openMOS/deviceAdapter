package org.fortiss.uaserver.device.lowlevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.fortiss.uaserver.device.instance.SkillState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CompositeSkillMethod extends SkillMethod {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	private List<NodeId> atomicSkillsNodeIds;
	protected List<SkillMethod> skills = new ArrayList<SkillMethod>();
	protected Map<String, String> inputArguments;
	protected Boolean ford;

	public CompositeSkillMethod() {

	};

	public CompositeSkillMethod(Boolean b) {
		this.ford = b;
	};

	public void setNodeIdsOfSkills(List<NodeId> atomicSkillsNodeIds) {
		this.atomicSkillsNodeIds = atomicSkillsNodeIds;
	}

	@Override
	protected void invokeDetached(AnnotationBasedInvocationHandler.InvocationContext context, String prdId,
			String prdType, Boolean searchForNextRecipe, String sr_id) throws Exception {
	    logger.info("composite triggered");

		long startTime = System.currentTimeMillis();
		
		for (SkillMethod skill : skills) {
			
			setKpiExecutionTime(0, true);
			Thread.sleep(100);
			setSkillState(SkillState.Running, true);
			logger.info("composite invokes {}", skill.getRecipeId());
			skill.invoke(context, prdId, prdType, false, sr_id);

			if (skill instanceof SkillMethod) {
				while (skill.getSkillState() != SkillState.Ready) {

					Thread.sleep(100);
				}
			}
		}

		Boolean allReady = false;
		do {
			allReady = true;
			for (SkillMethod atomic : skills) {
				if (!(atomic.getSkillState() == SkillState.Ready)) {
					allReady = false;
				}
			}
		} while (!allReady);

		long endTime = System.currentTimeMillis();
		setKpiExecutionTime((int) (endTime - startTime), false);
		setSkillState(SkillState.Ready, true);
	}

	public void initialize(Map<NodeId, SkillMethod> atomicSkills) {
		this.informationPortChild = getInformationPortChild(new NodeId(2, amlRecipePath));
		if (informationPortChild == null) {
			logger.error("Recipe has no information port child. Setting KPI is not supported.");
		}
		setKpiExecutionTime(0, true);
		setSkillState(SkillState.Ready, true);
		for (NodeId skillNodeId : atomicSkillsNodeIds) {
			skills.add(atomicSkills.get(skillNodeId));
		}
		for (Entry<NodeId, SkillMethod> entry : atomicSkills.entrySet()) {
			entry.getValue().initialize();

		}
	}


}
