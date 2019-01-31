package org.fortiss.masmec;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

import java.util.Optional;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.FolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.ServerNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegateChain;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.fortiss.ValueLoggingDelegate;
import org.fortiss.uaserver.common.BasicNamespace;
import org.slf4j.LoggerFactory;

class MasmecDummyNamespace extends BasicNamespace {


    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MasmecDummy.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);


    MasmecDummyNamespace(OpcUaServer server, UShort namespaceIndex, String namespaceUri) {
        super(server, namespaceIndex, namespaceUri);


        Optional<ServerNode> folder = server.getNodeMap().getNode(Identifiers.ObjectsFolder);
        if (!folder.isPresent()) {
            logger.error("Objects folder not found in namespace");
            return;
        }
        FolderNode folderNode = (FolderNode) folder.get();

        addManualLoadingBypass(folderNode);
        addManualLoadingLoad(folderNode);
        addTransport1ToLTS1(folderNode);
        addTransport1ToLTS2(folderNode);
        addLeakTest1(folderNode);
        addLeakTest2(folderNode);
        addTransport2LTS1ToT3(folderNode);
        addTransport2LTS2ToT3(folderNode);
        addTransport3ToGluing(folderNode);
        addTransport3ToUnloading(folderNode);
        addManualUnloading(folderNode);
        addGluing(folderNode);
        addTransport4ToShaker(folderNode);
        addShaker(folderNode);
        addTransport5ToLoading(folderNode);
    }

    private UaVariableNode addNode(FolderNode rootNode, String nodeName, NodeId typeId, Variant variant) {

        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
            .setNodeId(new NodeId(namespaceIndex, nodeName))
            .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
            .setUserAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
            .setBrowseName(new QualifiedName(namespaceIndex, nodeName))
            .setDisplayName(LocalizedText.english(nodeName))
            .setDataType(typeId)
            .setTypeDefinition(Identifiers.BaseDataVariableType)
            .build();

        node.setValue(new DataValue(variant));

        node.setAttributeDelegate(new ValueLoggingDelegate());

        server.getNodeMap().addNode(node);
        rootNode.addComponent(node);
        return node;
    }

    private void addTransport(FolderNode rootNode, String nameReady, String nameTransport) {


        // nodeReady
        UaVariableNode nodeReady = addNode(rootNode, nameReady, Identifiers.Boolean, new Variant(false));
        // nodeTransportToLTS1
        addNode(rootNode, nameTransport, Identifiers.Boolean, new Variant(false));

        final boolean[] ready = {true};

        Timer timer = new Timer();

        // change ready every 2 seconds
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ready[0] = !ready[0];
                nodeReady.setValue(new DataValue(new Variant(ready[0])));
            }
        }, 2000, 2000);

    }

    private void addTransport1ToLTS1(FolderNode rootNode) {
        addTransport(rootNode, "collegamento s7_6.db105.50,x1", "collegamento s7_6.db110.0,x4");
    }

    private void addTransport1ToLTS2(FolderNode rootNode) {
        addTransport(rootNode, "collegamento s7_6.db106.50,x1", "collegamento s7_6.db110.0,x5");
    }

    private void addTransport2LTS1ToT3(FolderNode rootNode) {
        addTransport(rootNode, "collegamento s7_7.db120.0,x2", "collegamento s7_7.db120.0,x0");
    }

    private void addTransport2LTS2ToT3(FolderNode rootNode) {
        addTransport(rootNode, "collegamento s7_7.db120.0,x2", "collegamento s7_7.db120.0,x1");
    }

    private void addTransport3ToGluing(FolderNode rootNode) {
        addTransport(rootNode, "collegamento s7_7.db102.50,x0", "collegamento s7_7.db121.0,x0");
    }

    private void addTransport3ToUnloading(FolderNode rootNode) {
        addTransport(rootNode, "collegamento s7_7.db103.50,x0", "collegamento s7_7.db121.0,x1");
    }

    private void addTransport4ToShaker(FolderNode rootNode) {
        addTransport(rootNode, "collegamento s7_7.db101.50,x1", "collegamento s7_7.db102.0,x2");
    }

    private void addTransport5ToLoading(FolderNode rootNode) {
        addTransport(rootNode, "collegamento s7_6.db110.50,x3", "collegamento s7_7.db101.0,x3");
    }

    private void addManualLoadingBypass(FolderNode rootNode) {
        // nodeBypass
        addNode(rootNode, "collegamento s7_6.db110.0,x2", Identifiers.Boolean, new Variant(false));
    }

    private void addManualLoadingLoad(FolderNode rootNode) {
        // nodeLoad
        UaVariableNode nodeLoad =
            addNode(rootNode, "collegamento s7_6.db110.0,x3", Identifiers.Boolean, new Variant(false));
        // nodeReady
        UaVariableNode nodeReady =
            addNode(rootNode, "collegamento s7_6.db110.50,x1", Identifiers.Boolean, new Variant(false));

        final boolean[] running = {false};
        AttributeDelegate delegateStart = AttributeDelegateChain.create(
            new AttributeDelegate() {
                @Override
                public void setValue(AttributeContext context, VariableNode node, DataValue value) throws UaException {
                    if ((Boolean) value.getValue().getValue() && !running[0]) {
                        logger.info("ManualLoad triggered, simulating button press in 1 second");
                        running[0] = true;
                        scheduler.schedule(() -> {
                            logger.info("Pressing ManualLoadButton");
                            running[0] = false;
                            nodeReady.setValue(new DataValue(new Variant(true)));
                            nodeLoad.setValue(new DataValue(new Variant(true)));
                        }, 1, TimeUnit.SECONDS);
                        // reset node ready after 2 seconds
                        scheduler.schedule(() -> {
                            nodeReady.setValue(new DataValue(new Variant(false)));
                        }, 2, TimeUnit.SECONDS);
                    }
                    node.setValue(value);
                }
            },
            ValueLoggingDelegate::new
        );
        nodeLoad.setAttributeDelegate(delegateStart);

    }


    private void addRunTriggerReady(FolderNode rootNode, String readyName, String runName, Consumer<Void> onRunDone) {

        final boolean[] running = {false};

        //nodeReady
        UaVariableNode nodeReady = addNode(rootNode, readyName, Identifiers.Boolean, new Variant(false));

        //nodeStart
        // when triggering start, wait a few seconds until ready
        UaVariableNode nodeStart = addNode(rootNode, runName, Identifiers.Boolean, new Variant(false));
        AttributeDelegate delegateStart = AttributeDelegateChain.create(
            new AttributeDelegate() {
                @Override
                public void setValue(AttributeContext context, VariableNode node, DataValue value) throws UaException {
                    logger.info("Start triggered, simulating run");
                    if ((Boolean) value.getValue().getValue() && !running[0]) {
                        nodeReady.setValue(new DataValue(new Variant(false)));
                        running[0] = true;
                        scheduler.schedule(() -> {
                            logger.info("Run done!");
                            running[0] = false;
                            if (onRunDone != null)
                                onRunDone.accept(null);
                            nodeReady.setValue(new DataValue(new Variant(true)));
                        }, 5, TimeUnit.SECONDS);
                        // reset node ready after 6 seconds
                        scheduler.schedule(() -> {
                            nodeReady.setValue(new DataValue(new Variant(false)));
                        }, 6, TimeUnit.SECONDS);
                    }
                    node.setValue(value);
                }
            },
            ValueLoggingDelegate::new
        );
        nodeStart.setAttributeDelegate(delegateStart);
    }

    private void addLeakTest1(FolderNode rootNode) {

        //nodeProgramNum
        addNode(rootNode, "collegamento s7_2.db100.0,w", Identifiers.Integer, new Variant(0));
        //nodeMinAttempts
        addNode(rootNode, "collegamento s7_2.db100.4,r", Identifiers.Float, new Variant(0f));
        //nodeMaxAttempts
        addNode(rootNode, "collegamento s7_2.db100.8,r", Identifiers.Float, new Variant(0f));

        UaVariableNode energyConsumption = addNode(rootNode, "collegamento s7_5.db100.12,r", Identifiers.Float, new Variant(0f));


        Timer timer = new Timer();
        Random r = new Random();

        // change ready every 2 seconds
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                float val = 100 + r.nextFloat() * (50);
                energyConsumption.setValue(new DataValue(new Variant(val)));
            }
        }, 0, 1000);

        final boolean[] isOk = {true};

        //nodeOk
        UaVariableNode nodeOk = addNode(rootNode, "collegamento s7_2.db100.36,x4", Identifiers.Boolean, new Variant(false));
        //nodeNok
        UaVariableNode nodeNok = addNode(rootNode, "collegamento s7_2.db100.36,x5", Identifiers.Boolean, new Variant(false));

        addRunTriggerReady(rootNode, "collegamento s7_2.db100.36,x3", "collegamento s7_2.db100.36,x0", aVoid -> {
            if (isOk[0]) {
                logger.info("Leaktest is OK");
                nodeOk.setValue(new DataValue(new Variant(true)));
                nodeNok.setValue(new DataValue(new Variant(false)));
            } else {
                logger.info("Leaktest is NOT OK");
                nodeOk.setValue(new DataValue(new Variant(false)));
                nodeNok.setValue(new DataValue(new Variant(true)));
            }
            isOk[0] = !isOk[0];
        });


    }

    private void addLeakTest2(FolderNode rootNode) {

        //nodeProgramNum
        addNode(rootNode, "collegamento s7_3.db100.0,w", Identifiers.Integer, new Variant(0));
        //nodeMinAttempts
        addNode(rootNode, "collegamento s7_3.db100.4,r", Identifiers.Float, new Variant(0f));
        //nodeMaxAttempts
        addNode(rootNode, "collegamento s7_3.db100.8,r", Identifiers.Float, new Variant(0f));


        UaVariableNode energyConsumption = addNode(rootNode, "collegamento s7_5.db100.16,r", Identifiers.Float, new Variant(0f));


        Timer timer = new Timer();
        Random r = new Random();

        // change ready every 2 seconds
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                float val = 100 + r.nextFloat() * (50);
                energyConsumption.setValue(new DataValue(new Variant(val)));
            }
        }, 0, 1000);

        final boolean[] isOk = {true};

        //nodeOk
        UaVariableNode nodeOk = addNode(rootNode, "collegamento s7_3.db100.36,x4", Identifiers.Boolean, new Variant(false));
        //nodeNok
        UaVariableNode nodeNok = addNode(rootNode, "collegamento s7_3.db100.36,x5", Identifiers.Boolean, new Variant(false));

        addRunTriggerReady(rootNode, "collegamento s7_3.db100.36,x3", "collegamento s7_3.db100.36,x0", aVoid -> {
            if (isOk[0]) {
                logger.info("Leaktest is OK");
                nodeOk.setValue(new DataValue(new Variant(true)));
                nodeNok.setValue(new DataValue(new Variant(false)));
            } else {
                logger.info("Leaktest is NOT OK");
                nodeOk.setValue(new DataValue(new Variant(false)));
                nodeNok.setValue(new DataValue(new Variant(true)));
            }
            isOk[0] = !isOk[0];
        });


    }

    private void addGluing(FolderNode rootNode) {
        // gluingDone
        addNode(rootNode, "collegamento s7_7.db102.0,x1", Identifiers.Boolean, new Variant(false));
    }

    private void addManualUnloading(FolderNode rootNode) {
        // nodeReady
        UaVariableNode nodeReady =
            addNode(rootNode, "collegamento s7_7.db103.50,x2", Identifiers.Boolean, new Variant(false));

        final boolean[] ready = {true};

        Timer timer = new Timer();

        // change ready every 2 seconds
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                ready[0] = !ready[0];
                nodeReady.setValue(new DataValue(new Variant(ready[0])));
            }
        }, 2000, 2000);

    }

    private void addShaker(FolderNode rootNode) {

        //nodeAngle
        addNode(rootNode, "collegamento s7_7.db101.100,r", Identifiers.Float, new Variant(0f));
        //nodeSpeed
        addNode(rootNode, "collegamento s7_7.db101.104,r", Identifiers.Float, new Variant(0f));
        //nodeAcceleration
        addNode(rootNode, "collegamento s7_7.db101.108,r", Identifiers.Float, new Variant(0f));
        //nodeNumOfRepetitions
        addNode(rootNode, "collegamento s7_7.db101.112,w", Identifiers.Integer, new Variant(0));

        UaVariableNode energyConsumption = addNode(rootNode, "collegamento s7_7.db101.2,r", Identifiers.Double, new Variant(0.0));


        Timer timer = new Timer();
        Random r = new Random();

        // change ready every 2 seconds
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                double val = 400 + r.nextDouble() * (50);
                energyConsumption.setValue(new DataValue(new Variant(val)));
            }
        }, 0, 1000);


        addRunTriggerReady(rootNode, "collegamento s7_7.db101.50,x2", "collegamento s7_7.db101.0,x2", null);

    }
}
