package org.fortiss.uaserver.device.instance;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.annotations.UaOutputArgument;
import org.eclipse.milo.opcua.sdk.server.api.MethodInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.api.ServerNodeMap;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.FolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.Out;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.fortiss.uaserver.common.InfoModelGenerator;
import org.fortiss.uaserver.common.MsbGenericComponent;
import org.fortiss.uaserver.device.lowlevel.CompositeSkillMethod;
import org.fortiss.uaserver.device.lowlevel.SkillMethod;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMLParser {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    String adapterId;

    private File aml;
    private SAXBuilder builder;
    private Document doc;
    private OpcUaServer server;
    private MsbGenericComponent msbComponent;
    public static final int NAMESPACE_IDX = 2;
    private CompositeSkillMethod compositeSkillMethod = null;

    private Map<String, String> inpArgs = new HashMap<String, String>();
    private Map<String, NodeId> amlIdNodeId = new HashMap<String, NodeId>();
    private Map<NodeId, NodeId> nodeParent = new HashMap<NodeId, NodeId>();
    private Map<NodeId, SkillMethod> nodeIdskillMethod = new HashMap<NodeId, SkillMethod>();
    private Map<String, List<String>> skills4devicelist = new HashMap<String, List<String>>();

    private List<Argument> outArgs = new ArrayList<Argument>();
    Function<String, String> getDeviceEndpoint;

    public AMLParser(File amlFile, MsbGenericComponent msbComponent, Function<String, String> getDeviceEndpoint) {
        this.doc = null;
        this.aml = amlFile;
        this.getDeviceEndpoint = getDeviceEndpoint;
        this.builder = new SAXBuilder();
        this.msbComponent = msbComponent;
        this.server = msbComponent.getServer();
        populateNamespace();
    }

    public void populateNamespace() {
        server.getNodeMap().get(new NodeId(NAMESPACE_IDX, 338))
                .setBrowseName(new QualifiedName(NAMESPACE_IDX, aml.getName()));
        server.getNodeMap().get(new NodeId(NAMESPACE_IDX, 338)).setDisplayName(new LocalizedText("en", aml.getName()));
        elementsLists();
    }

    private void addCustomNodes(NodeId parentNode) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(NAMESPACE_IDX, "changeSkillRecipe"))
                .setBrowseName(new QualifiedName(NAMESPACE_IDX, "changeSkillRecipe"))
                .setDisplayName(new LocalizedText("en-US", "changeSkillRecipe"))
                .setDescription(LocalizedText.english("Change the parameter of a skill")).build();

        try {
            AnnotationBasedInvocationHandler invocationHandler = AnnotationBasedInvocationHandler
                    .fromAnnotatedObject(server.getNodeMap(), new ChangeSkillRecipe());

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);

            addReferencesBothDirections(parentNode, Identifiers.HasComponent, methodNode.getNodeId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void elementsLists() {
        try {
            if (null == doc) {
                doc = (Document) builder.build(aml);
            }
            getChildren(doc.getRootElement());
            addPnPNodes();
            addReferences(doc.getRootElement());
            addSkill();
            hidePnPdevicesAndSkills(doc.getRootElement());
        } catch (IOException ioex) {
            System.out.println(ioex.getLocalizedMessage());
        } catch (JDOMException jdomex) {
            System.out.println(jdomex.getLocalizedMessage());
        }
    }

    public String getAdapterId() {
        return adapterId;
    }

    public NodeId getParentOfNode(NodeId node) {
        return nodeParent.get(node);
    }

    private void addPnPNodes() {
        UaObjectNode hiddenDevices = new UaObjectNode(server.getNodeMap(), new NodeId(NAMESPACE_IDX, "HiddenDevices"),
                new QualifiedName(NAMESPACE_IDX, "HiddenDevices"), new LocalizedText("en", "HiddenDevices"));
        server.getNodeMap().addNode(hiddenDevices);
        addReferencesBothDirections(Identifiers.ObjectsFolder, Identifiers.Organizes, hiddenDevices.getNodeId());

        UaObjectNode hiddenSkills = new UaObjectNode(server.getNodeMap(), new NodeId(NAMESPACE_IDX, "HiddenSkills"),
                new QualifiedName(NAMESPACE_IDX, "HiddenSkills"), new LocalizedText("en", "HiddenSkills"));
        server.getNodeMap().addNode(hiddenSkills);
        addReferencesBothDirections(Identifiers.ObjectsFolder, Identifiers.Organizes, hiddenSkills.getNodeId());

    }

    private void hidePnPdevicesAndSkills(Element element) {

        for (Element e : element.getChildren()) {
            if (e.getAttributeValue("Name") != null && e.getAttributeValue("Name").contains("VisionSystem")) {
                if (e.getAttributeValue("ID") != null) {
                    logger.info("VS id: {}", e.getAttributeValue("ID"));
                    List<String> skills4vision = new ArrayList<String>();
                    skills4vision.add(
                            "Pre-Demonstrator_InstanceHierarchy/AssemblySystem/WorkStation_Ford/SC2: TaskFull_Recipe");
                    skills4vision.add(
                            "Introsys_Demonstrator_InstanceHierarchy/AssemblySystem/WorkStation_VW/SC2: VW_TaskFull_Recipe");
                    skills4devicelist.put(e.getAttributeValue("ID"), skills4vision);
                    setDeviceNodesVisibility(e.getAttributeValue("ID"), false, false);
                }
            }

            if (e.getChildren().size() > 0) {
                hidePnPdevicesAndSkills(e);

            }
        }
    }

    private void getChildren(Element element) {

        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(NAMESPACE_IDX, "getNodeIdByAmlId"))
                .setBrowseName(new QualifiedName(NAMESPACE_IDX, "getNodeIdByAmlId"))
                .setDisplayName(new LocalizedText(null, "getNodeIdByAmlId")).setDescription(LocalizedText.english(""))
                .build();

        try {
            AnnotationBasedInvocationHandler invocationHandler = AnnotationBasedInvocationHandler
                    .fromAnnotatedObject(server.getNodeMap(), new GetNodeIdFromAmlId());

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);

            FolderNode folderNode = (FolderNode) server.getNodeMap().getNode(Identifiers.ObjectsFolder).get();
            addReferencesBothDirections(folderNode.getNodeId(), Identifiers.HasComponent, methodNode.getNodeId());

            // addReferencesBothDirections(methodNode.getNodeId(), Identifiers.HasComponent,
            // folderNode.getNodeId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (Element e : element.getChildren()) {
            if (e.getName().equals("InstanceHierarchy")) {
                addFolder(e, new NodeId(NAMESPACE_IDX, 342), new NodeId(1, 5005));
            } else if (e.getName().equals("InterfaceClassLib")) {
                addFolder(e, new NodeId(NAMESPACE_IDX, 345), new NodeId(1, 5008));
            } else if (e.getName().equals("RoleClassLib")) {
                addFolder(e, new NodeId(NAMESPACE_IDX, 344), new NodeId(1, 5003));
            } else if (e.getName().equals("SystemUnitClassLib")) {
                addFolder(e, new NodeId(NAMESPACE_IDX, 343), new NodeId(1, 5004));
            } else if (e.getName().equals("InternalElement")) {
                if (!server.getNodeMap().containsNodeId(new NodeId(NAMESPACE_IDX, e.getAttributeValue("Name")))) {
                    addObject(e);
                }
                if ((e.getAttributeValue("ID") != null)) {
                    addVariable(e, "ID", e.getAttributeValue("ID"), new NodeId(NAMESPACE_IDX, getPrefix(e)),
                            Identifiers.HasComponent);
                    if (e.getChildren().size() == 0) {
                        addVariable(e, "ID", e.getAttributeValue("RefBaseSystemUnitPath"),
                                new NodeId(NAMESPACE_IDX, getPrefix(e)), Identifiers.HasComponent);
                    }
                }
            } else if (e.getName().equals("SystemUnitClass") && element.getName().equals("InternalElement")) {
                if (!server.getNodeMap().containsNodeId(new NodeId(NAMESPACE_IDX, e.getAttributeValue("Name")))) {
                    addObjectType(e, new NodeId(NAMESPACE_IDX, getPrefix(element)), Identifiers.HasTypeDefinition);
                }

            } else if (e.getName().equals("RoleClass") && (element.getName().equals("RoleClassLib"))) {
                addObjectType(e, new NodeId(NAMESPACE_IDX, getPrefix(element)), Identifiers.HasComponent);
            } else if (e.getName().equals("RoleClass") && (element.getName().equals("RoleClass"))) {
                if (!server.getNodeMap().containsNodeId(new NodeId(NAMESPACE_IDX, e.getAttributeValue("Name")))) {
                    addObjectType(e, new NodeId(NAMESPACE_IDX, getPrefix(element)), Identifiers.Organizes);
                }
            } else if (e.getName().equals("RoleClass") && (element.getName().equals("SystemUnitClass"))) {
                addObjectType(e, new NodeId(NAMESPACE_IDX, getPrefix(element)), new NodeId(1, 4001));
            } else if (e.getName().equals("SystemUnitClass") && (element.getName().equals("SystemUnitClassLib"))) {
                addObjectType(e, new NodeId(NAMESPACE_IDX, getPrefix(element)), Identifiers.HasComponent);
            } else if (e.getName().equals("SystemUnitClass") && (element.getName().equals("SystemUnitClass"))) {
                if (!server.getNodeMap().containsNodeId(new NodeId(NAMESPACE_IDX, e.getAttributeValue("Name")))) {
                    addObjectType(e, new NodeId(NAMESPACE_IDX, getPrefix(element)), Identifiers.Organizes);
                }
            } else if (e.getName().equals("Attribute")) {
                if (!server.getNodeMap().containsNodeId(new NodeId(NAMESPACE_IDX, e.getAttributeValue("Name")))) {
                    addVariable(e, new NodeId(NAMESPACE_IDX, getPrefix(element)), Identifiers.HasComponent);
                }
                if ((e.getAttributeValue("Unit") != null)) {
                    addVariable(e, "Unit", e.getAttributeValue("Unit"), new NodeId(NAMESPACE_IDX, getPrefix(element)),
                            Identifiers.HasComponent);
                }
            } else if (e.getName().equals("ExternalInterface") && !element.getName().equals("ExternalInterface")) {
                if (!server.getNodeMap().containsNodeId(new NodeId(NAMESPACE_IDX, e.getAttributeValue("Name")))) {
                    addObject(e);
                }
            } else if (e.getName().equals("ExternalInterface") && element.getName().equals("ExternalInterface")) {
                if (!server.getNodeMap().containsNodeId(new NodeId(NAMESPACE_IDX, e.getAttributeValue("Name")))) {
                    addObject(e);
                }
            }
            if (e.getChildren().size() > 0) {
                getChildren(e);

            }

        }

    }

    private void addReferences(Element element) {
        for (Element e : element.getChildren()) {
            if ((e.getName().equals("SupportedRoleClass") || e.getName().equals("RoleRequirements"))
                    && (element.getName().equals("InternalElement") || element.getName().equals("SystemUnitClass"))) {
                String name = e.getAttributeValue("RefRoleClassPath") == null
                        ? e.getAttributeValue("RefBaseRoleClassPath")
                        : e.getAttributeValue("RefRoleClassPath");
                addReferencesBothDirections(

                        new NodeId(NAMESPACE_IDX, getPrefix(e.getParentElement())), new NodeId(1, 4001),
                        server.getNodeMap().getNode(new NodeId(NAMESPACE_IDX, name)).get().getNodeId());

            }
            if ((e.getName().equals("SystemUnitClass") && (element.getName().equals("SystemUnitClass"))
                    || (e.getName().equals("RoleClass") && element.getName().equals("RoleClass"))
                            && e.getAttributeValue("RefBaseClassPath") != null)) {
                String name = e.getAttributeValue("RefBaseClassPath");
                try {
                    addReferencesBothDirections(
                            server.getNodeMap().getNode(new NodeId(NAMESPACE_IDX, getPrefix(e))).get().getNodeId(),

                            // according to companion spec it should be HasSubtype here.. using
                            // TypeDefeinition as it seems more natural in our case.. re-consider, if needed
                            Identifiers.HasTypeDefinition, new NodeId(NAMESPACE_IDX, name));

                } catch (NoSuchElementException exc) {

                }
            }
            if (e.getName().equals("InterfaceClass") && (e.getAttributeValue("RefBaseClassPath") != null)) {
                String name = e.getAttributeValue("RefBaseClassPath");
                try {
                    addReferencesBothDirections(

                            new NodeId(NAMESPACE_IDX, getPrefix(e)), Identifiers.HasSubtype,
                            server.getNodeMap().getNode(new NodeId(NAMESPACE_IDX, name)).get().getNodeId());
                } catch (NoSuchElementException exc) {

                }

            }
            if ((e.getName().equals("SystemUnitClass") && (element.getName().equals("SystemUnitClass"))
                    || (e.getName().equals("RoleClass") && element.getName().equals("RoleClass"))
                            && e.getAttributeValue("RefBaseClassPath") != null)) {
                String name = e.getAttributeValue("RefBaseClassPath");
                try {
                    addReferencesBothDirections(

                            server.getNodeMap().getNode(new NodeId(NAMESPACE_IDX, name)).get().getNodeId(),
                            Identifiers.HasSubtype, new NodeId(NAMESPACE_IDX, getPrefix(e)));
                } catch (NoSuchElementException exc) {

                }
            }
            if (e.getName().equals("InternalElement") && (e.getAttributeValue("RefBaseSystemUnitPath") != null)) {
                String name = e.getAttributeValue("RefBaseSystemUnitPath");

                try {
                    addReferencesBothDirections(

                            new NodeId(NAMESPACE_IDX, getPrefix(e)), Identifiers.HasTypeDefinition,
                            server.getNodeMap().getNode(new NodeId(NAMESPACE_IDX, name)).get().getNodeId());
                } catch (NoSuchElementException exc) {

                }

            }
            if (e.getName().equals("InternalLink")) {
                String name = e.getAttributeValue("RefRoleClassPath") == null
                        ? e.getAttributeValue("RefBaseRoleClassPath")
                        : e.getAttributeValue("RefRoleClassPath");
                try {
                    if (e.getAttributeValue("RefPartnerSideA")
                            .substring(e.getAttributeValue("RefPartnerSideA").indexOf(":") + 1)
                            .equals("RequirementConnector")) {

                        addReferencesBothDirections(
                                amlIdNodeId.get(e.getAttributeValue("RefPartnerSideB").substring(0,
                                        e.getAttributeValue("RefPartnerSideB").indexOf(":"))),
                                new NodeId(1, 4002), amlIdNodeId.get(e.getAttributeValue("RefPartnerSideA").substring(0,
                                        e.getAttributeValue("RefPartnerSideA").indexOf(":"))));
                        logger.info("internal link: {} >> {} added",
                                amlIdNodeId.get(e.getAttributeValue("RefPartnerSideB").substring(0,
                                        e.getAttributeValue("RefPartnerSideB").indexOf(":"))).getIdentifier(),
                                amlIdNodeId.get(e.getAttributeValue("RefPartnerSideA").substring(0,
                                        e.getAttributeValue("RefPartnerSideA").indexOf(":"))).getIdentifier());
                    }
                } catch (NullPointerException npe) {

                }

            }
            if (e.getChildren().size() > 0) {
                addReferences(e);
            }
        }
    }

    private String getDaId(NodeId skillNode, String nodeId) throws Exception {
        String da_id = "";
        try {
            Optional<ServerNode> da_id_node = server.getNodeMap().getNode(new NodeId(NAMESPACE_IDX, nodeId));
            if (da_id_node.isPresent() && da_id_node.get() instanceof UaVariableNode) {
                UaVariableNode n = (UaVariableNode) da_id_node.get();
                da_id = (String) n.getValue().getValue().getValue();
            } else {
                throw new Exception("Could not find 'DA_ID' node in namespace " + nodeId);
            }
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return da_id;
    }

    private void addDAStateNode(String id) {
        String tmp = id.substring(0, id.lastIndexOf("/"));
        String tmp2 = tmp.substring(tmp.lastIndexOf("/") + 1);

        NodeId daStateNodeId = new NodeId(NAMESPACE_IDX, "DeviceAdapterState/" + tmp2);
        if (!server.getNodeMap().getNode(daStateNodeId).isPresent()) {
            UaVariableNode daStateNode = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                    .setNodeId(daStateNodeId).setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(new QualifiedName(NAMESPACE_IDX, "DeviceAdapterState"))
                    .setDisplayName(LocalizedText.english("DeviceAdapterState")).setDataType(Identifiers.String)
                    .setTypeDefinition(Identifiers.BaseDataVariableType).setValue(new DataValue(new Variant("")))
                    .build();
            server.getNodeMap().put(new NodeId(NAMESPACE_IDX, "DeviceAdapterState/" + tmp2), daStateNode);
            addReferencesBothDirections(new NodeId(NAMESPACE_IDX, tmp), Identifiers.HasComponent,
                    daStateNode.getNodeId());
        }
    }

    private void addSkill() {
        String da_id = "";
        Map<String, String> da_id_map = new HashMap<>();
        Map<NodeId, SkillMethod> allAtomicSkills = new HashMap<NodeId, SkillMethod>();
        Map<NodeId, CompositeSkillMethod> allCompositeSkills = new HashMap<NodeId, CompositeSkillMethod>();
        String lboroStationName = "";
        for (Entry<NodeId, ServerNode> entry : server.getNodeMap().entrySet()) {
            for (Reference r : entry.getValue().getReferences()) {
                if (r.getReferenceTypeId().equals(new NodeId(1, 4001))
                        && r.getTargetNodeId().getIdentifier().toString().equals("openMOSRoleClassLib/Equipment")
                        && r.getSourceNodeId().getIdentifier().toString().contains("InstanceHierarchy")) {

                    String id = r.getSourceNodeId().getIdentifier().toString();
                    NodeId equipmentNode = new NodeId(NAMESPACE_IDX, id);
                    try {
                        da_id = getDaId(equipmentNode, id + "ID");
                        da_id_map.put(equipmentNode.getIdentifier().toString(), da_id);
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                }
                if (r.getReferenceTypeId().equals(new NodeId(1, 4001))
                        && r.getTargetNodeId().getIdentifier().toString()
                                .equals("openMOSRoleClassLib/Equipment/SubSystem/WorkStation")
                        && r.getSourceNodeId().getIdentifier().toString().contains("InstanceHierarchy")) {
                    String id = r.getSourceNodeId().getIdentifier().toString();
                    NodeId equipmentNode = new NodeId(NAMESPACE_IDX, id);
                    try {
                        adapterId = getDaId(equipmentNode, id + "ID");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    for (Reference equipementRef : server.getNodeMap().getNode(equipmentNode).get().getReferences()) {
                        if (equipementRef.getTargetNodeId().getIdentifier().toString().contains("name")) {
                            Optional<ServerNode> nameNode = server.getNodeMap()
                                    .getNode(equipementRef.getTargetNodeId());
                            if (nameNode.isPresent() && nameNode.get() instanceof UaVariableNode) {
                                UaVariableNode n = (UaVariableNode) nameNode.get();
                                lboroStationName = (String) n.getValue().getValue().getValue();
                            }
                        }
                    }
                }
            }
        }
        for (Entry<NodeId, ServerNode> entry : server.getNodeMap().entrySet()) {
            for (Reference r : entry.getValue().getReferences()) {
                if (r.getReferenceTypeId().equals(new NodeId(1, 4001))
                        && r.getTargetNodeId().getIdentifier().toString().equals("openMOSRoleClassLib/Skill")
                        && r.getSourceNodeId().getIdentifier().toString().contains("InstanceHierarchy")) {
                    String id = r.getSourceNodeId().getIdentifier().toString();
                    List<NodeId> skillDefs = new ArrayList<NodeId>();
                    List<NodeId> nodeIdsOfSkills = new ArrayList<NodeId>();
                    NodeId nextNode = server.getNodeMap().getNode(r.getSourceNodeId()).get().getNodeId();
                    Boolean composite = false;
                    do {
                        skillDefs = getTargetsByRefType(nextNode, Identifiers.HasTypeDefinition);
                        if (!skillDefs.isEmpty()) {
                            nextNode = skillDefs.get(0);
                            if (skillDefs.contains(new NodeId(2, "openMOSSystemUnitClassLib/Skill/AtomicSkill"))) {
                                composite = false;
                            }
                            if (skillDefs.contains(new NodeId(2, "openMOSSystemUnitClassLib/Skill/CompositeSkill"))) {
                                composite = true;
                                NodeId compositeNodeId = server.getNodeMap().getNode(r.getSourceNodeId()).get()
                                        .getNodeId();
                                List<NodeId> skillComponents = getTargetsByRefType(
                                        server.getNodeMap().getNode(r.getSourceNodeId()).get().getNodeId(),
                                        Identifiers.HasComponent);
                                for (NodeId node : skillComponents) {
                                    List<NodeId> requirementsNodes = getTargetsByRefType(node,
                                            Identifiers.HasTypeDefinition);
                                    for (NodeId reqNode : requirementsNodes) {
                                        if (reqNode.getIdentifier()
                                                .equals("openMOSSystemUnitClassLib/Requirement/SkillRequirement")) {
                                            List<NodeId> skillsForRequirement = getTargetsByRefType(node,
                                                    new NodeId(1, 4002));
                                            for (NodeId skill : skillsForRequirement) {
                                                try {
                                                    nodeIdsOfSkills.add(skill);
                                                } catch (Exception e) {
                                                    logger.error(
                                                            "Something went wrong while adding composite skill {} >> {}",
                                                            compositeNodeId, skill);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } while (!skillDefs.isEmpty() || nextNode.getIdentifier().toString().contains("AtomicSkill")
                            || nextNode.getIdentifier().toString().contains("CompositeSkill"));
                    addDAStateNode(id);
                    NodeId skillNode = new NodeId(NAMESPACE_IDX, id);
                    String className = "";
                    String recipe_id = "";
                    String name = r.getSourceNodeId().getIdentifier().toString()
                            .substring(r.getSourceNodeId().getIdentifier().toString().lastIndexOf("/") + 1);

                    Optional<ServerNode> recipe_id_node = server.getNodeMap()
                            .getNode(new NodeId(NAMESPACE_IDX, skillNode.getIdentifier().toString() + "ID"));
                    if (recipe_id_node.isPresent() && recipe_id_node.get() instanceof UaVariableNode) {
                        UaVariableNode n = (UaVariableNode) recipe_id_node.get();
                        recipe_id = (String) n.getValue().getValue().getValue();
                    }

                    for (Reference rr : server.getNodeMap().getNode(r.getSourceNodeId().expanded()).get()
                            .getReferences()) {
                        if (rr.getReferenceTypeId().equals(Identifiers.HasTypeDefinition)) {
                            className = server.getNodeMap().getNode(rr.getTargetNodeId()).get().getBrowseName()
                                    .getName();
                        }
                    }
                    if (!server.getNodeMap().getNode(skillNode).get().getReferences().contains(new Reference(skillNode,
                            Identifiers.HasTypeDefinition,
                            new NodeId(2, "openMOSSystemUnitClassLib/Requirement/SkillRequirement").expanded(),
                            server.getNodeMap()
                                    .get(new NodeId(2, "openMOSSystemUnitClassLib/Requirement/SkillRequirement"))
                                    .getNodeClass(),
                            true))) {
                        String equipmentIdentifier = skillNode.getIdentifier().toString().substring(0,
                                skillNode.getIdentifier().toString().lastIndexOf("/"));
                        String aux_DAID = da_id_map.get(equipmentIdentifier);
                        SkillMethod skillMethod = instantiateSkill(className, skillNode, name, aux_DAID, recipe_id, id,
                                composite, this.aml.getName().contains("Ford"), lboroStationName);

                        if (skillMethod instanceof SkillMethod) {
                            allAtomicSkills.put(skillNode, skillMethod);
                        }
                        if (skillMethod instanceof CompositeSkillMethod) {
                            ((CompositeSkillMethod) skillMethod).setNodeIdsOfSkills(nodeIdsOfSkills);
                            allCompositeSkills.put(skillNode, (CompositeSkillMethod) skillMethod);
                        }
                        UaMethodNode methodNode = new UaMethodNode(server.getNodeMap(),
                                new NodeId(NAMESPACE_IDX, "InvokeSkill/" + name),
                                new QualifiedName(NAMESPACE_IDX, "InvokeSkill"), new LocalizedText(null, "InvokeSkill"),
                                LocalizedText.english("Triggers the underlying skill of the device."),
                                UInteger.valueOf(0), UInteger.valueOf(0), true, true);

                        UaVariableNode methodStateNode = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                                .setNodeId(new NodeId(NAMESPACE_IDX, "SkillState/" + name))
                                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                                .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                                .setBrowseName(new QualifiedName(NAMESPACE_IDX, "SkillState"))
                                .setDisplayName(LocalizedText.english("SkillState")).setDataType(Identifiers.String)
                                .setTypeDefinition(Identifiers.BaseDataVariableType)
                                .setValue(new DataValue(new Variant(""))).build();

                        ServerNodeMap nodeManager = server.getNodeMap();

                        if (skillMethod != null) {
                            try {
                                AnnotationBasedInvocationHandler invocationHandler;
                                invocationHandler = AnnotationBasedInvocationHandler.fromAnnotatedObject(nodeManager,
                                        skillMethod);

                                methodNode.setProperty(UaMethodNode.InputArguments,
                                        invocationHandler.getInputArguments());
                                methodNode.setProperty(UaMethodNode.OutputArguments,
                                        invocationHandler.getOutputArguments());
                                methodNode.setInvocationHandler(invocationHandler);
                            } catch (Exception e) {
                            }
                            nodeManager.put(methodNode.getNodeId(), methodNode);
                            nodeManager.put(methodStateNode.getNodeId(), methodStateNode);
                            if (!composite) {
                                skillMethod.initialize();
                            }
                        }

                        /*
                         * Upper layer creates the reference for accessing the node. This is done below
                         * skillNode.addReference(new Reference( skillNode.getNodeId(),
                         * Identifiers.HasComponent, methodNode.getNodeId().expanded(),
                         * methodNode.getNodeClass(), true ));
                         */

                        server.getNodeMap().put(new NodeId(NAMESPACE_IDX, "InvokeSkill/" + name), methodNode);
                        server.getNodeMap().put(new NodeId(NAMESPACE_IDX, "SkillState/" + name), methodStateNode);

                        // methodNode.setOutputArguments(outputArguments);

                        addReferencesBothDirections(skillNode, Identifiers.HasComponent, methodNode.getNodeId());
                        addReferencesBothDirections(skillNode, Identifiers.HasComponent, methodStateNode.getNodeId());

                        // methodNode.setOutputArguments();//TODO:handle output arguments if needed

                        addReferencesBothDirections(r.getSourceNodeId(), Identifiers.HasComponent,
                                methodNode.getNodeId());

                    }
                }
                if (r.getReferenceTypeId().equals(new NodeId(1, 4001))
                        && (r.getTargetNodeId().getIdentifier().toString()
                                .equals("openMOSRoleClassLib/Equipment/Module")
                                || r.getTargetNodeId().getIdentifier().toString()
                                        .equals("openMOSRoleClassLib/Equipment/SubSystem/WorkStation"))
                        && r.getSourceNodeId().getIdentifier().toString().contains("InstanceHierarchy")) {
                    addCustomNodes(r.getSourceNodeId());
                }

            }

        }
        for (Entry<NodeId, CompositeSkillMethod> compositeSkill : allCompositeSkills.entrySet()) {
            compositeSkill.getValue().initialize(allAtomicSkills);
        }
        addAGVResourcesNode();
    }

    private void addAGVResourcesNode() {
        if (this.aml.getName().contains("AGV")) {
            UaFolderNode kpiFolder = new UaFolderNode(server.getNodeMap(), new NodeId(NAMESPACE_IDX, "KPIs"),
                    new QualifiedName(NAMESPACE_IDX, "KPIs"), new LocalizedText("en", "KPIs"));
            server.getNodeMap().put(kpiFolder.getNodeId(), kpiFolder);
            addReferencesBothDirections(Identifiers.ObjectsFolder, Identifiers.HasComponent, kpiFolder.getNodeId());

            UaVariableNode agvResourcesNode = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                    .setNodeId(new NodeId(NAMESPACE_IDX, "Resources"))
                    .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                    .setBrowseName(new QualifiedName(NAMESPACE_IDX, "Resources"))
                    .setDisplayName(LocalizedText.english("Resources")).setDataType(Identifiers.String)
                    .setTypeDefinition(Identifiers.BaseDataVariableType).setValue(new DataValue(new Variant("")))
                    .build();
            server.getNodeMap().put(agvResourcesNode.getNodeId(), agvResourcesNode);
            addReferencesBothDirections(kpiFolder.getNodeId(), Identifiers.HasComponent, agvResourcesNode.getNodeId());
            agvResourcesNode.setValue(new DataValue(new Variant(new Integer(7))));
        }

    }

    private SkillMethod instantiateSkill(String className, NodeId skillNode, String name, String da_id,
            String recipe_id, String id, Boolean composite, Boolean ford, String lboroStationName) {
        Object method = null;
        SkillMethod skillMethod = null;
        List<NodeId> skillComponents = getTargetsByRefType(skillNode, Identifiers.HasComponent);
        List<NodeId> parameterPortOptional = filter(skillComponents, "openMOSRoleClassLib/ParameterPort");
        if (!composite) {
            try {
                method = Class.forName("org.fortiss.uaserver.device.lowlevel.skills.atomic." + className).newInstance();
            } catch (Exception e1) {
                try {
                    method = Class.forName("org.fortiss.uaserver.device.lowlevel.skills.atomic." + className)
                            .getDeclaredConstructor(String.class).newInstance(lboroStationName);
                } catch (Exception e) {
                    logger.error("Exception {} for {}", e.getClass(), className);
                }
            }
        } else {
            try {
                method = Class.forName("org.fortiss.uaserver.device.lowlevel.skills.composite." + className)
                        .newInstance();
            } catch (Exception e1) {
                try {
                    method = Class.forName("org.fortiss.uaserver.device.lowlevel.skills.composite." + className)
                            .getDeclaredConstructor(String.class).newInstance(lboroStationName);
                } catch (Exception e) {
                    logger.error("Exception {} for {}", e.getClass(), className);
                }
            }
        }
        if (method instanceof SkillMethod) {
            Map<NodeId, String> pars = new HashMap<NodeId, String>();
            if (!composite) {
                skillMethod = (SkillMethod) method;
                if (!parameterPortOptional.isEmpty()) {
                    NodeId parameterPort = parameterPortOptional.get(0);
                    skillMethod.setMethod(msbComponent, da_id, recipe_id, id,
                            getParameterNamesFromNodeIds(addSkillPars(parameterPort, pars)));
                    logger.info("instantiated {} skill {} of class {} with following pars: {}",
                            composite ? "composite" : "atomic", name, className, addSkillPars(parameterPort, pars));
                } else {
                    skillMethod.setMethod(msbComponent, da_id, recipe_id, id, new HashMap<String, String>());
                    logger.info("instantiated {} skill {} of class {} with following pars: no parameters",
                            composite ? "composite" : "atomic", name, className);
                }

            } else {
                skillMethod = (CompositeSkillMethod) method;
                ((CompositeSkillMethod) skillMethod).setMethod(msbComponent, da_id, recipe_id, id, null);
                logger.info("instantiated {} skill {} of class {}", composite ? "composite" : "atomic", name,
                        className);
            }
        }
        return skillMethod;

    }

    private List<NodeId> filter(List<NodeId> components, String filter) {
        List<NodeId> ret = new ArrayList<NodeId>();
        for (NodeId sn : components) {
            for (NodeId componentTypes : getTargetsByRefType(sn, new NodeId(1, 4001))) {
                if (componentTypes.equals(new NodeId(2, filter))) {
                    ret.add(sn);
                }
            }
        }
        return ret;
    }

    private Map<NodeId, String> addSkillPars(NodeId node, Map<NodeId, String> pars) {
        List<NodeId> parameterPortComponents = getTargetsByRefType(node, Identifiers.HasComponent);
        List<NodeId> parametersList = filter(parameterPortComponents, "openMOSRoleClassLib/Parameter");
        for (NodeId parameters : parametersList) {
            if (getTargetsByRefType(parameters, Identifiers.HasTypeDefinition)
                    .contains(new NodeId(NAMESPACE_IDX, "openMOSSystemUnitClassLib/Parameter/AtomicParameter"))) {
                for (NodeId parameter : getTargetsByRefType(parameters, Identifiers.HasComponent)) {
                    // ignoring id's and units, not needed for device adapter skill instantiation
                    if (!server.getNodeMap().getNode(parameter).get().getBrowseName().getName().equals("ID")
                            && !server.getNodeMap().getNode(parameter).get().getBrowseName().getName().equals("Unit")) {
                        try {
                            pars.put(parameter, (String) ((UaVariableNode) server.getNodeMap().getNode(parameter).get())
                                    .getValue().getValue().getValue().toString());
                        } catch (ClassCastException e) {
                            logger.error("Can not add a parameter. Not a variable node: {}", parameter);
                        }
                    }
                }
            }
            if (getTargetsByRefType(parameters, Identifiers.HasTypeDefinition)
                    .contains(new NodeId(NAMESPACE_IDX, "openMOSSystemUnitClassLib/Parameter/CompositeParameter"))) {
                addSkillPars(parameters, pars);
            }
        }
        // }
        return pars;
    }

    private CompletableFuture<CallMethodResult> changePars(Map<NodeId, String> oldPars, Variant[] newPars) {
        int i = 0;
        for (Entry<NodeId, String> entry : oldPars.entrySet()) {
            try {
                UaVariableNode parNode = (UaVariableNode) server.getNodeMap().getNode(entry.getKey()).get();
                parNode.setValue(DataValue.valueOnly(newPars[i]));
                logger.info("setting old {} to new value {}", entry.getKey(), newPars[i++]);
            } catch (NoSuchElementException e) {
                logger.error("No value for nodeId {}", entry.getKey());
            }

        }
        CompletableFuture<CallMethodResult> fut = new CompletableFuture<CallMethodResult>();

        return fut;
    }

    private List<ServerNode> getSourcesByRefType(NodeId source, NodeId referenceType) {
        List<ServerNode> retNodes = new ArrayList<ServerNode>();
        for (Reference r : server.getNodeMap().getNode(source.expanded()).get().getReferences()) {
            if (r.getReferenceTypeId().equals(referenceType)) {
                retNodes.add(server.getNodeMap().getNode(r.getSourceNodeId()).get());
            }
        }
        return retNodes;
    }

    private List<NodeId> getTargetsByRefType(NodeId source, NodeId referenceType) {
        List<NodeId> retNodes = new ArrayList<NodeId>();
        for (Reference r : server.getNodeMap().getNode(source.expanded()).get().getReferences()) {
            if (r.getReferenceTypeId().equals(referenceType) && r.isForward()) {
                retNodes.add(server.getNodeMap().getNode(r.getTargetNodeId()).get().getNodeId());
            }
        }
        return retNodes;
    }

    private void addObject(Element e) {
        UaObjectNode internalElement = new UaObjectNode(server.getNodeMap(), new NodeId(NAMESPACE_IDX, getPrefix(e)),
                new QualifiedName(NAMESPACE_IDX, e.getAttributeValue("Name")),
                new LocalizedText("en", e.getAttributeValue("Name")));
        server.getNodeMap().addNode(internalElement);
        NodeId parentId = new NodeId(NAMESPACE_IDX, getPrefix(e.getParentElement()));
        addReferencesBothDirections(parentId, Identifiers.HasComponent, internalElement.getNodeId());
        nodeParent.put(internalElement.getNodeId(), parentId);
    }

    private void addFolder(Element e, NodeId parentFolder, NodeId nonHierarchicalRef) {
        UaFolderNode folder = new UaFolderNode(server.getNodeMap(), new NodeId(NAMESPACE_IDX, getPrefix(e)),
                new QualifiedName(NAMESPACE_IDX, e.getAttributeValue("Name")),
                new LocalizedText("en", e.getAttributeValue("Name")));
        server.getNodeMap().addNode(folder);
        addReferencesBothDirections(nonHierarchicalRef, Identifiers.Organizes, folder.getNodeId());
        addReferencesBothDirections(parentFolder, Identifiers.HasComponent, folder.getNodeId());
    }

    private void addObjectType(Element e, NodeId parentNodeId, NodeId referenceType) {
        UaObjectTypeNode systemUnitClass = new UaObjectTypeNode(server.getNodeMap(),
                new NodeId(NAMESPACE_IDX, getPrefix(e)), new QualifiedName(NAMESPACE_IDX, e.getAttributeValue("Name")),
                new LocalizedText("en", e.getAttributeValue("Name")),
                new LocalizedText("en", e.getAttributeValue("Name")), UInteger.valueOf(0), UInteger.valueOf(0), false);
        server.getNodeMap().addNode(systemUnitClass);
        addReferencesBothDirections(parentNodeId, referenceType, systemUnitClass.getNodeId());
        if ((e.getAttributeValue("ID") != null)) {
            addVariable(e, "ID", e.getAttributeValue("ID"), new NodeId(NAMESPACE_IDX, getPrefix(e)),
                    Identifiers.HasComponent);

        }
    }

    private void addVariable(Element e, NodeId parentNodeId, NodeId referenceType) {

        UaVariableNode varNode = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(NAMESPACE_IDX, getPrefix(e)))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(NAMESPACE_IDX, e.getAttributeValue("Name")))
                .setDisplayName(LocalizedText.english(e.getAttributeValue("Name"))).setDataType(Identifiers.String)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .setValue(e.getChild("Value") == null ? new DataValue(new Variant(0))
                        : new DataValue(new Variant(e.getChild("Value").getValue())))
                .build();
        varNode.setDataType(nodeIdFromAttribute(e.getAttributeValue("AttributeDataType"), varNode));

        if (e.getChild("Value") != null) {
            varNode.setValue(new DataValue(new Variant(e.getChild("Value").getValue())));
        }

        server.getNodeMap().addNode(varNode);
        addReferencesBothDirections(parentNodeId, referenceType, varNode.getNodeId());
    }

    private void addVariable(Element e, String name, String value, NodeId parentNodeId, NodeId referenceType) {
        UaVariableNode varNode = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(NAMESPACE_IDX, getPrefix(e) + name))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(NAMESPACE_IDX, name)).setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.String).setTypeDefinition(Identifiers.BaseDataVariableType)
                .setValue(new DataValue(new Variant(value))).build();

        if (name == "ID") {
            amlIdNodeId.put(value, parentNodeId);
        }

        server.getNodeMap().addNode(varNode);
        addReferencesBothDirections(parentNodeId, referenceType, varNode.getNodeId());
    }

    private String getPrefix(Element e) {
        String id = "";
        try {
            while (!(e.getName().equals("SystemUnitClassLib") || e.getName().equals("RoleClassLib")
                    || e.getName().equals("InstanceHierarchy"))) {
                id = e.getAttributeValue("Name") + "/" + id;
                e = e.getParentElement();
            }
        } catch (NullPointerException exc) {

        }
        try {
            id = e.getAttributeValue("Name") + "/" + id;

            return id.substring(0, id.lastIndexOf("/"));
        } catch (NullPointerException exc) {
            return "";
        }
    }

    private NodeId nodeIdFromAttribute(String attribute, UaVariableNode varNode) {
        if (attribute != null) {
            switch (attribute) {
            case "xs:string":
                varNode.setValue(new DataValue(new Variant("")));
                return Identifiers.String;
            case "xs:anyURI":
                varNode.setValue(new DataValue(new Variant("")));
                return Identifiers.String;
            case "xs:integer":
                varNode.setValue(new DataValue(new Variant(Integer.valueOf(0))));
                return Identifiers.Integer;
            case "xs:int":
                varNode.setValue(new DataValue(new Variant(Integer.valueOf(0))));
                return Identifiers.Integer;
            case "xs:float":
                varNode.setValue(new DataValue(new Variant(Float.valueOf(0))));
                return Identifiers.Float;
            case "xs:double":
                varNode.setValue(new DataValue(new Variant(Double.valueOf(0.0))));
                return Identifiers.Double;
            case "xs:unsignedInt":
                varNode.setValue(new DataValue(new Variant(UInteger.valueOf(0))));
                return Identifiers.UInteger;
            default:
                varNode.setValue(new DataValue(new Variant("")));
                return Identifiers.String;
            }
        }
        varNode.setValue(new DataValue(new Variant("")));
        return Identifiers.String;
    }

    private Map<String, String> getParameterNamesFromNodeIds(Map<NodeId, String> params) {
        Map<String, String> nameValueMap = new HashMap<String, String>();
        for (Entry<NodeId, String> entry : params.entrySet()) {
            nameValueMap.put(entry.getKey().getIdentifier().toString().substring(
                    entry.getKey().getIdentifier().toString().indexOf("ParameterPort/") + "ParameterPort/".length()),
                    entry.getValue());
        }
        return nameValueMap;
    }

    private List<SkillMethod> getAllSkillsForCompositeNode(NodeId compositeNodeId) {
        logger.info("looking for all links for {}", compositeNodeId);
        List<SkillMethod> skillsList = new ArrayList<SkillMethod>();
        for (Entry<NodeId, SkillMethod> entry : nodeIdskillMethod.entrySet()) {
            if (entry.getKey().equals(compositeNodeId)) {
                skillsList.add(entry.getValue());
            }
        }
        return skillsList;
    }

    private Map<NodeId, SkillMethod> getNodeIdskillMethodMap() {
        return nodeIdskillMethod;
    }

    private UaMethodNode addChangeSkillNode(NodeId nodeId, String name) {
        // return null;
        UaMethodNode changeMethodNode = new UaMethodNode(server.getNodeMap(),
                new NodeId(NAMESPACE_IDX, "EditRecipe/" + name), new QualifiedName(NAMESPACE_IDX, "ChangeRecipe"),
                new LocalizedText(null, "EditRecipe"),
                LocalizedText.english("Edits (or creates new) a recipe of the device."), UInteger.valueOf(0),
                UInteger.valueOf(0), true, true);

        // Argument input[] = new Argument[addSkillPars(skillNode).size()];
        // int i = 0;
        // for (Entry<NodeId, String> param : addSkillPars(skillNode).entrySet()) {
        // input[i++] = new Argument(
        // param.getKey().getIdentifier().toString()
        // .substring(param.getKey().getIdentifier().toString().lastIndexOf("/") +
        // 1),
        // Identifiers.String, -2, null, new LocalizedText("en", "Skill parameter"));
        // }
        Argument input[] = {
                new Argument("Recipe", Identifiers.String, -2, null, new LocalizedText("en", "Recipe string")) };
        changeMethodNode.setProperty(UaMethodNode.InputArguments, input);
        EditRecipeMethod editRecipeMethod = new EditRecipeMethod();
        try {

            MethodInvocationHandler invocationHandler;
            changeMethodNode.setInputArguments(input);
            invocationHandler = AnnotationBasedInvocationHandler.fromAnnotatedObject(server.getNodeMap(),
                    editRecipeMethod);
            changeMethodNode.setInvocationHandler(invocationHandler);

        } catch (Exception e) {
        }
        return changeMethodNode;
    }

    public NodeId amlIdToNodeId(String amlId) {
        return amlIdNodeId.get(amlId);
    }

    private void addReferencesBothDirections(NodeId node1, NodeId refType, NodeId node2) {
        server.getNodeMap().addReference(new Reference(node1, refType, node2.expanded(),
                server.getNodeMap().getNode(node2).get().getNodeClass(), true));
        if (refType.equals(Identifiers.HasComponent) || refType.equals(Identifiers.Organizes)) {
            server.getNodeMap().addReference(new Reference(node2, refType, node1.expanded(),
                    server.getNodeMap().getNode(node1).get().getNodeClass(), false));

        }
    }

    public class GetNodeIdFromAmlId {

        @UaMethod
        public void invoke(InvocationContext context,

                @UaInputArgument(name = "key", description = "string key") String key,

                @UaOutputArgument(name = "getNodeId", description = "") Out<NodeId> nodeId) {

            nodeId.set(amlIdNodeId.get(key));
        }

    }

    public void setDeviceNodesVisibility(String deviceId, boolean shown, boolean skill) {
        NodeId deviceNode;
        if (!skill) {
            deviceNode = this.amlIdToNodeId(deviceId);
        } else {
            deviceNode = new NodeId(NAMESPACE_IDX, deviceId);
        }
        logger.info("{}", deviceNode.getIdentifier());
        if (deviceNode.getIdentifier() instanceof String) {
            if (((String) deviceNode.getIdentifier()).contains("InstanceHierarchy")) {
                logger.warn("Setting device node visibility for " + deviceId + " -> " + (shown ? "visible" : "hidden"));

                NodeId deviceParentFolder = this.getParentOfNode(deviceNode);
                if (deviceParentFolder == null) {
                    logger.error("Could not get parent of device node");
                    return;
                }
                NodeId hiddenDevices = new NodeId(2, "HiddenDevices");
                NodeId hiddenSkills = new NodeId(2, "HiddenSkills");
                NodeId hiddenParentFolder;
                if (skill) {
                    hiddenParentFolder = hiddenSkills;
                } else {
                    hiddenParentFolder = hiddenDevices;
                }

                if (shown) {
                    for (Reference ref : server.getNodeMap().getNode(hiddenParentFolder).get().getReferences()) {
                        if (ref.getTargetNodeId().equals(deviceNode.expanded())) {
                            server.getNodeMap().getNode(hiddenParentFolder).get().removeReference(ref);
                            addReferencesBothDirections(deviceParentFolder, ref.getReferenceTypeId(), deviceNode);
                        }
                    }
                    List<String> skills = skills4devicelist.get(deviceId);
                    if (skills != null) {
                        for (String skillIdString : skills) {
                            setDeviceNodesVisibility(skillIdString, true, true);
                        }
                    }
                } else {
                    for (Reference ref : server.getNodeMap().getNode(deviceParentFolder).get().getReferences()) {
                        if (ref.getTargetNodeId().equals(deviceNode.expanded())) {
                            server.getNodeMap().getNode(deviceParentFolder).get().removeReference(ref);
                            logger.info("{}", ref);
                            addReferencesBothDirections(hiddenParentFolder, ref.getReferenceTypeId(), deviceNode);
                        }
                    }
                    List<String> skills = skills4devicelist.get(deviceId);
                    if (skills != null) {
                        for (String skillIdString : skills) {
                            setDeviceNodesVisibility(skillIdString, false, true);
                        }
                    }
                }

            }
        } else {
            logger.error("node identifier is not a string");
        }
    }

    public class ChangeSkillRecipe {

        @UaMethod
        public void invoke(InvocationContext context,
                @UaInputArgument(name = "recipe", description = "Recipe as string") String paRecipe,
                @UaOutputArgument(name = "result", description = "Result of the operation. True for valid, false otherwise") Out<Boolean> returnValue)
                throws UaException {
            try {

                logger.info("A new recipe has arrive. Applying...");
                InputStream stream = new ByteArrayInputStream(paRecipe.getBytes("UTF-8"));
                Document recipeDoc = (Document) builder.build(stream);
                if (!("recipe" == recipeDoc.getRootElement().getName())) {
                    logger.error("The receive string is not a recipe");
                    returnValue.set(false);
                    return;
                }

                // uniqueId -- mandatory
                String recipeId = "";
                // name -- mandatory
                String recipeName = "";
                // uniqueId -- mandatory
                String skillId = "";

                // List<ParameterSetting> parameterSettings, -- optional
                Map<String, String> parameterSettings = new HashMap<String, String>();
                // List<SkillRequirement> skillRequirements, -- optional
                Map<String, String> skillRequirements = new HashMap<String, String>();

                Element found = getElementFromPath(recipeDoc.getRootElement(), "uniqueId");
                if (null == found) {
                    logger.error("RecipeId was not found in the sent recipe.");
                    returnValue.set(false);
                    return;
                }
                recipeId = found.getValue();

                found = getElementFromPath(recipeDoc.getRootElement(), "name");
                if (null == found) {
                    logger.error("Recipe Name was not found in the sent recipe");
                    returnValue.set(false);
                    return;
                } else {
                    recipeName = found.getValue();
                }

                found = getElementFromPath(recipeDoc.getRootElement(), "skill_id");
                if (null == found) {
                    logger.error("SkillId was not found in the sent recipe");
                    returnValue.set(false);
                    return;
                } else {
                    skillId = found.getValue();
                }

                for (Element e : recipeDoc.getRootElement().getChildren()) {
                    if (e.getName().equals("skillRequirements")) {
                        skillRequirements.put(getElementFromPath(e, "uniqueId").getValue(),
                                getElementFromPath(e, "recipeIDs").getValue());
                    }
                }

                for (Element e : recipeDoc.getRootElement().getChildren()) {
                    if (e.getName().equals("parameterSettings")) {
                        parameterSettings.put(getElementFromPath(e, "uniqueId").getValue(),
                                getElementFromPath(e, "value").getValue());
                    }
                }

                Boolean newRecipe = false;
                NodeId recipeFound = null;
                for (Entry<NodeId, ServerNode> entry : server.getNodeMap().entrySet()) {
                    for (Reference r : entry.getValue().getReferences()) {
                        if (r.getReferenceTypeId().equals(new NodeId(1, 4001))
                                && r.getTargetNodeId().getIdentifier().toString().equals("openMOSRoleClassLib/Skill")
                                && entry.getValue().getDisplayName().getText().equals(recipeName)) {
                            recipeFound = entry.getValue().getNodeId();
                            UaVariableNode idNode = (UaVariableNode) server.getNodeMap()
                                    .getNode(new NodeId(NAMESPACE_IDX,
                                            (String) entry.getValue().getNodeId().getIdentifier() + "ID"))
                                    .get();
                            if (!((String) idNode.getValue().getValue().getValue()).equals(recipeId)) {
                                logger.error("The skill for recipe {} {} was not found", recipeName, recipeId);
                            }
                        }
                    }
                }

                if (null == recipeFound) {
                    logger.error("The skill wasn't found in the namespace {}", recipeName);
                    // returnValue.set(false);
                    // return;

                    newRecipe = true;
                }

                if (newRecipe) {
                    Element internalElement = null;
                    String query = "//SystemUnitClass[@ID='" + skillId + "']";
                    try {
                        XPathFactory factory = XPathFactory.instance();
                        XPathExpression<Element> xpe = factory.compile(query, Filters.element());
                        for (Element e : xpe.evaluate(doc)) {
                            internalElement = e;
                            internalElement.setName("InternalElement");
                            internalElement.setAttribute("Name", recipeName);
                            // Element parameterPort = getElementFromPath(e,
                            // "InternalElement;Name=ParameterPort");
                            for (Entry<String, String> entry : parameterSettings.entrySet()) {
                                String queryPars = "//InternalElement[@ID='" + entry.getKey() + "']";
                                XPathFactory factoryPars = XPathFactory.instance();
                                XPathExpression<Element> xpePars = factoryPars.compile(queryPars, Filters.element());
                                for (Element el : xpePars.evaluate(internalElement)) {
                                    el.addContent(new Element("Value").setText(entry.getValue()));
                                    System.out.println("value");
                                }

                            }
                           
                        }
                        
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException iae) {
                        iae.printStackTrace();
                    }
                    returnValue.set(true);
//                    new XMLOutputter().output(internalElement, System.out);
                    return;
                }

                String id = (String) recipeFound.getIdentifier();

                String toLookDevice = "InstanceHierarchy;Name=" + id.substring(0, id.indexOf("/")) + "/";
                id = id.substring(id.indexOf("/") + 1);

                do {
                    toLookDevice = toLookDevice + "InternalElement;Name=" + id.substring(0, id.indexOf("/")) + "/";
                    id = id.substring(id.indexOf("/") + 1);
                } while (-1 != id.indexOf("/"));

                String toLookSkill = toLookDevice + "InternalElement;Name=" + id;

                // toLook = toLook + "InternalElement;Name=" + id
                // + "/InternalElement;Name=ParameterPort/InternalElement;Name=" + parameterName
                // + "/Attribute/";

                found = getElementFromPath(doc.getRootElement(), toLookSkill); // look for attribute to get the name
                                                                               // of
                                                                               // the

                if (null == found) {
                    logger.error("The attribute tag of the parameter wasn't found");
                    returnValue.set(false);
                    return;
                }

                for (Element e : found.getChildren()) {
                    for (String s : parameterSettings.keySet()) {
                        if (e.getAttributeValue("Name") != null
                                && e.getAttributeValue("Name").contains("ParameterPort")) {
                            for (Element parameter : e.getChildren()) {
                                if (parameter.getAttributeValue("ID") != null
                                        && parameter.getAttributeValue("ID").equals(s)) {
                                    parameter.getChild("Attribute").getChild("Value").setText(parameterSettings.get(s));
                                }
                            }
                        }
                    }
                }

                found = getElementFromPath(doc.getRootElement(), toLookDevice);
                for (Element e : found.getChildren()) {
                    for (String s : skillRequirements.keySet()) {
                        if (e.getName().equals("InternalLink")) {
                            if (e.getAttributeValue("RefPartnerSideB")
                                    .substring(0, e.getAttributeValue("RefPartnerSideB").indexOf(":")).equals(s)) {
                                e.setAttribute("RefPartnerSideA", skillRequirements.get(s) + ":RequirementConnector");
                            }
                        }
                    }
                }

                logger.info("Writing recipe to AML file...");
                PrintWriter writeToFile = new PrintWriter(aml);
                writeToFile.print(new XMLOutputter().outputString(doc));
                writeToFile.close();
                // msbComponent.updateDevice(getAdapterId());
                for (Entry<NodeId, ServerNode> entry : server.getNodeMap().entrySet()) {
                    if (entry.getKey().getNamespaceIndex().intValue() == NAMESPACE_IDX
                            && entry.getKey().getIdentifier().toString().contains("InstanceHierarchy")) {
                        server.getNodeMap().remove(entry.getKey());
                    }
                }
                new InfoModelGenerator().create("amls/Opc.Ua.AMLBaseTypes.NodeSet2.xml", server);
                new InfoModelGenerator().create("amls/Opc.Ua.AMLLibraries.NodeSet2.xml", server);
                populateNamespace();
                returnValue.set(true);
            } catch (

            IOException ioex) {
                System.out.println(ioex.getLocalizedMessage());
                returnValue.set(false);
            } catch (JDOMException jdomex) {
                System.out.println(jdomex.getLocalizedMessage());
                returnValue.set(false);
            }
        }

        private Element getElementFromPath(Element paRoot, String paPath) { // the path can be:
                                                                            // TAGNAME;ATTRIBUTE=VALUE/TAGNAME1;ATTRIBUTE1=VALUE1

            // Remove the slash at the end and the one at the beginning
            if (paPath.endsWith("/")) {
                paPath = paPath.substring(0, paPath.length() - 1);
            }
            if (paPath.startsWith("/")) {
                paPath = paPath.substring(1);
            }

            String tagName = "";
            String attrName = "";
            String attrValue = "";
            String remaining = "";
            int indexOfSlash = paPath.indexOf("/");
            if (-1 != indexOfSlash) {
                tagName = paPath.substring(0, indexOfSlash);
                remaining = paPath.substring(indexOfSlash + 1);
            } else {
                tagName = paPath;
            }

            int indexOfAttr = tagName.indexOf(";");
            if (-1 != indexOfAttr) {
                attrName = tagName.substring(indexOfAttr + 1);
                tagName = tagName.substring(0, indexOfAttr);

                int indeofOfValue = attrName.indexOf("=");
                if (-1 != indeofOfValue) {
                    attrValue = attrName.substring(indeofOfValue + 1);
                    attrName = attrName.substring(0, indeofOfValue);
                } else {
                    logger.error("Internal error: Wrong usage of getContentFromPath function");
                }

            }

            for (Element e : paRoot.getChildren()) {
                if (e.getName().equals(tagName)) {
                    boolean found = false;
                    if ("" != attrName) {
                        if (null != e.getAttribute(attrName) && e.getAttribute(attrName).getValue().equals(attrValue)) {
                            found = true;
                        }

                    } else {
                        found = true;
                    }
                    if (found) {
                        if ("" == remaining) {
                            return e;
                        } else {
                            return getElementFromPath(e, remaining);
                        }
                    }
                }
            }
            return null; // if not found this will be reached
        }

    }

}
