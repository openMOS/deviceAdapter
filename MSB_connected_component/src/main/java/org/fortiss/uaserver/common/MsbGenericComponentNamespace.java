package org.fortiss.uaserver.common;

import static org.fortiss.uaserver.device.DeviceUaServer.amlFile;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.fortiss.uaserver.common.type.SkillTypeNode;
import org.fortiss.uaserver.device.instance.AMLParser;
import org.fortiss.uaserver.device.instance.ConnectToAbove;
import org.fortiss.uaserver.device.instance.MasmecRaspiNamespace;
import org.fortiss.uaserver.device.instance.MasmecTestNamespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;

public abstract class MsbGenericComponentNamespace extends BasicNamespace {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private final MsbGenericComponent msbComponent;

	private SkillTypeNode skillTypeNode;

	public MsbGenericComponentNamespace(MsbGenericComponent msbComponent, UShort namespaceIndex, String namespaceUri) {
		super(msbComponent.getServer(), namespaceIndex, namespaceUri);
		this.msbComponent = msbComponent;

		createSkillBaseType();
		new InfoModelGenerator().create("amls/Opc.Ua.AMLBaseTypes.NodeSet2.xml", server);
		new InfoModelGenerator().create("amls/Opc.Ua.AMLLibraries.NodeSet2.xml", server);

		createCAEXFileTypes();
		createTestNamespace();
	}

	/**
	 * Create the SkillBaseType as a child of ObjectTypes
	 */
	private void createSkillBaseType() {

		NodeId skillBaseTypeNodeId = new NodeId(getNamespaceIndex(), "SkillBaseType");

		try {
			getServer().getUaNamespace().addReference(Identifiers.ObjectTypesFolder, Identifiers.Organizes, true,
					skillBaseTypeNodeId.expanded(), NodeClass.ObjectType);
		} catch (UaException e) {
			getLogger().error("Error adding reference to Object type folder.", e);
		}

		QualifiedName browseName = new QualifiedName(getNamespaceIndex(), "SkillBaseType");
		LocalizedText displayName = new LocalizedText("en", "SkillBaseType");

		skillTypeNode = new SkillTypeNode(getNodeMap(), skillBaseTypeNodeId, browseName, displayName,
				new LocalizedText("en", "Generic type for describing skill elements"), UInteger.valueOf(0L),
				UInteger.valueOf(0L), true, getNamespaceIndex());

		getNodeMap().put(skillBaseTypeNodeId, skillTypeNode);

	}

	/**
	 * Create the static structure for corresponding CAEX Files
	 */
	private void createCAEXFileTypes() {
		new AMLParser(new File(amlFile), this.msbComponent, this::getDeviceAdapterEndpoint);
	}

	private String getDeviceAdapterEndpoint(String recipeName) {
		String hostname = null;

		try {
			InetAddress addr;
			addr = InetAddress.getLocalHost();
			hostname = addr.getHostName();
		} catch (UnknownHostException ignore) {
		}
		if (hostname != null) {
			if (hostname.compareToIgnoreCase("fortiss-n-094") == 0) {
				// return "opc.tcp://192.168.17.194:55101";
				// return "opc.tcp://127.0.0.1:4841/vrep/server";
				return "opc.tcp://172.18.3.51:16665"; // Introsys Gazebo
			} else if (hostname.compareToIgnoreCase("fortiss-n-087") == 0) {
				// return "opc.tcp://172.18.3.51:16665";
				// return "opc.tcp://localhost:55001";
				return "opc.tcp://127.0.0.1:55101";
				// return "opc.tcp://127.0.0.1:4841/vrep/server";
				// return "opc.tcp://172.18.3.51:16665"; // Introsys Gazebo
				// return "opc.tcp://172.18.3.80:49320"; // Introsys Robots
			}
		}

		return "opc.tcp://172.18.3.80:49320";
	}

	private void createTestNamespace() {
		new MasmecTestNamespace(server);
		ConnectToAbove cta = new ConnectToAbove(server);
		cta.prepareConnectToAbove();

		new MasmecRaspiNamespace(server);
	}

	protected UaObjectNode addFoldersToRoot(UaNode root, String path) {
		if (path.startsWith("/")) {
			path = path.substring(1, path.length());
		}
		String[] elements = path.split("/");

		LinkedList<UaObjectNode> folderNodes = processPathElements(Lists.newArrayList(elements), Lists.newArrayList(),
				Lists.newLinkedList());

		UaObjectNode firstNode = folderNodes.getFirst();

		if (!server.getNodeMap().containsKey(firstNode.getNodeId())) {
			server.getNodeMap().put(firstNode.getNodeId(), firstNode);

			server.getNodeMap().get(root.getNodeId()).addReference(new Reference(root.getNodeId(),
					Identifiers.Organizes, firstNode.getNodeId().expanded(), firstNode.getNodeClass(), true));

			logger.debug("Added reference: {} -> {}", root.getNodeId(), firstNode.getNodeId());
		}

		PeekingIterator<UaObjectNode> iterator = Iterators.peekingIterator(folderNodes.iterator());

		while (iterator.hasNext()) {
			UaObjectNode node = iterator.next();

			server.getNodeMap().putIfAbsent(node.getNodeId(), node);

			if (iterator.hasNext()) {
				UaObjectNode next = iterator.peek();

				if (!server.getNodeMap().containsKey(next.getNodeId())) {
					server.getNodeMap().put(next.getNodeId(), next);

					server.getNodeMap().get(node.getNodeId()).addReference(new Reference(node.getNodeId(),
							Identifiers.Organizes, next.getNodeId().expanded(), next.getNodeClass(), true));
					logger.debug("Added reference: {} -> {}", node.getNodeId(), next.getNodeId());
				}
			}
		}

		return folderNodes.getLast();
	}

	private LinkedList<UaObjectNode> processPathElements(List<String> elements, List<String> path,
			LinkedList<UaObjectNode> nodes) {
		if (elements.size() == 1) {
			String name = elements.get(0);
			String prefix = String.join("/", path) + "/";
			if (!prefix.startsWith("/")) {
				prefix = "/" + prefix;
			}

			UaObjectNode node = UaObjectNode.builder(server.getNodeMap())
					.setNodeId(new NodeId(namespaceIndex, prefix + name))
					.setBrowseName(new QualifiedName(namespaceIndex, name)).setDisplayName(LocalizedText.english(name))
					.setTypeDefinition(Identifiers.FolderType).build();

			nodes.add(node);

			return nodes;
		} else {
			String name = elements.get(0);
			String prefix = String.join("/", path) + "/";
			if (!prefix.startsWith("/")) {
				prefix = "/" + prefix;
			}

			UaObjectNode node = UaObjectNode.builder(server.getNodeMap())
					.setNodeId(new NodeId(namespaceIndex, prefix + name))
					.setBrowseName(new QualifiedName(namespaceIndex, name)).setDisplayName(LocalizedText.english(name))
					.setTypeDefinition(Identifiers.FolderType).build();

			nodes.add(node);
			path.add(name);

			return processPathElements(elements.subList(1, elements.size()), path, nodes);
		}
	}

	public Logger getLogger() {
		return logger;
	}

	public NodeId getSkillBaseTypeNodeId() {
		return skillTypeNode.getNodeId();
	}

	public SkillTypeNode getSkillTypeNode() {
		return skillTypeNode;
	}
}
