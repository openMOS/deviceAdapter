package org.fortiss.uaserver.device.instance;

import java.io.File;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.jdom2.input.SAXBuilder;

public class MasmecRaspiNamespace {
	private File aml;
	private SAXBuilder builder;
	private OpcUaServer server;
	private int NAMESPACE_IDX = 2;
	public MasmecRaspiNamespace(OpcUaServer server) {
		this.builder = new SAXBuilder();
		this.server = server;
		//addSkill(server.getNodeMap(), new NodeId(2, "raspiTestSkill"));
	}


//	private void addSkill(ServerNodeMap nodeManager, NodeId skillNode) {
//		UaMethodNode methodNode = new UaMethodNode(server.getNodeMap(),
//				new NodeId(NAMESPACE_IDX, "InvokeSkill/" + "raspiTestSkill"), new QualifiedName(NAMESPACE_IDX, "InvokeSkill"),
//				new LocalizedText(null, "InvokeSkill"),
//				LocalizedText.english("Triggers the underlying skill of the device."), UInteger.valueOf(0),
//				UInteger.valueOf(0), true, true);
//
//		String id = skillNode.getIdentifier().toString();
//		
//		
//		RaspiTestSkillMethod method = new RaspiTestSkillMethod();
//
//		AnnotationBasedInvocationHandler invocationHandler;
//		try {
//			invocationHandler = AnnotationBasedInvocationHandler
//					.fromAnnotatedObject(nodeManager, method);
//			methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
//			methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
//			methodNode.setInvocationHandler(invocationHandler);
//
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		
//		nodeManager.put(methodNode.getNodeId(), methodNode);
//
//		server.getNodeMap().put(new NodeId(NAMESPACE_IDX, "InvokeSkill/" + "raspiTestSkill"), methodNode);
//
//		server.getNodeMap().put(methodNode.getNodeId(), methodNode);
//		
//		NodeId objectsFolder = Identifiers.ObjectsFolder;
//		
//		Optional<ServerNode> objects = server.getNodeMap().getNode(objectsFolder);
//				try {
//			server.getUaNamespace().addReference(Identifiers.ObjectsFolder, Identifiers.Organizes, true,
//					methodNode.getNodeId().expanded(), NodeClass.Object);
//		} catch (UaException e) {
//		}
//
//
//			
//		server.getNodeMap().addReference(new Reference(skillNode, Identifiers.HasComponent,
//				methodNode.getNodeId().expanded(), methodNode.getNodeClass(), true));
//	}


}