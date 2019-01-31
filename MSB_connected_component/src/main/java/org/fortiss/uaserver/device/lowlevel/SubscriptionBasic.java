package org.fortiss.uaserver.device.lowlevel;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import javax.swing.JTextField;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaSubscription;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MonitoringMode;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodResult;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoredItemCreateRequest;
import org.eclipse.milo.opcua.stack.core.types.structured.MonitoringParameters;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.fortiss.uaserver.common.MsbGenericComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SubscriptionBasic implements Client {

    protected final Logger logger;
    protected Map<String, OpcUaClient> clientMap = new HashMap<String, OpcUaClient>();
    protected OpcUaClient clientTest = null;
    protected boolean Connection;
    protected UaSubscription subscription;
    protected List<UaMonitoredItem> items;
    protected boolean doSubscription = false;
    protected ArrayList<ReadValueId> readValueList = new ArrayList<ReadValueId>();
    private final AtomicLong clientHandles = new AtomicLong(1L);

    private Map<String, Integer> nsIdxMap = new HashMap<>();

    public static String S7_NAMESPACE_URI = "S7:";

    protected void onSubscriptionValue(UaMonitoredItem item, DataValue value) {
    }

    protected void setSubscription() throws InterruptedException, ExecutionException {
        // verify if exists any subscription activate, and if it does, remove it
        // if (items != null) {
        // subscription.deleteMonitoredItems(items);
        // logger.info("Deleted old Subscription. Setting new Subscription");
        // }

        ArrayList<MonitoredItemCreateRequest> requestList = getMonitoredItemRequestList();

        BiConsumer<UaMonitoredItem, Integer> onItemCreated = (item, id) -> item
                .setValueConsumer(this::onSubscriptionValue);

        items = subscription.createMonitoredItems(TimestampsToReturn.Both, newArrayList(requestList), onItemCreated)
                .get();

        // items.forEach((item) -> {
        // if (item.getStatusCode().isGood()) {
        // logger.info("1 item created for nodeId={}",
        // item.getReadValueId().getNodeId());
        // } else {
        // logger.warn("failed to create item for nodeId={} (status={})",
        // item.getReadValueId().getNodeId(),
        // item.getStatusCode());
        // }
        // });
    }

    protected abstract void initAfterConnect();

    public CompletableFuture<Boolean> startConnectionWithSubscription(String endpointUrl) throws Exception {
        logger.info(endpointUrl);
        this.doSubscription = true;
        CompletableFuture<Boolean> future = startConnection(endpointUrl);

        return future;
    }

    public CompletableFuture<Boolean> startConnection(String endpointUrl) throws Exception {
        synchronized (this) {
            CompletableFuture<Boolean> futureClientReady = new CompletableFuture<>();
            if (!clientMap.containsKey(endpointUrl)) {
                ClientRunner cr = new ClientRunner(endpointUrl, this, futureClientReady);
                try {
                    clientTest = cr.createClient();
                } catch (Exception e) {
                    logger.error(endpointUrl);
                }
                clientMap.put(endpointUrl, clientTest);
                cr.run();
                return futureClientReady;
            }
            CompletableFuture<Boolean> f = new CompletableFuture<>();
            run(clientMap.get(endpointUrl), f);
            return f;
        }
    }

    public SubscriptionBasic(Class clazz) {
        this.logger = LoggerFactory.getLogger(clazz);
    }

    @Override
    public void run(OpcUaClient client, CompletableFuture<Boolean> futureClientReady) throws Exception {
        // synchronous connect
        clientTest = client;
        clientTest.connect().get();
        initAfterConnect();
        Connection = true;
        // create a subscription and a monitored item
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        // exec.scheduleAtFixedRate(() -> {

        if (doSubscription) {
            try {
                subscription = clientTest.getSubscriptionManager().createSubscription(100.0).get();
                setSubscription();
            } catch (InterruptedException | ExecutionException ex) {
                logger.error("Subscription failed", ex);
            }
        }
        // }, 0, 100, TimeUnit.MILLISECONDS);
        futureClientReady.complete(true);
    }

    public void exitProgram(Logger logger) {
        if (clientTest != null) {
            clientTest.disconnect();
            logger.info("Client disconnect!");
        }
    }

    public void writeProgramNode(NodeId node, JTextField textF) {
        Variant v = new Variant(textF.getText());
        DataValue dv = new DataValue(v, null, null);
        clientTest.writeValue(node, dv);
        try {
            logger.info("{}", StatusCodes.lookup(clientTest.writeValue(node, dv).get().getValue()).get()[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void writeProgramNode(NodeId node, Variant v) {
       // Variant v = new Variant(textF);
        DataValue dv = new DataValue(v, null, null);
        clientTest.writeValue(node, dv);
        try {
            logger.info("{}", StatusCodes.lookup(clientTest.writeValue(node, dv).get().getValue()).get()[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeProgramNode(NodeId node, UInteger textF) {
        Variant v = new Variant(textF);
        DataValue dv = new DataValue(v, null, null);
        clientTest.writeValue(node, dv);
        try {
            logger.info("{}", StatusCodes.lookup(clientTest.writeValue(node, dv).get().getValue()).get()[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeProgramNode(NodeId node, Integer textF) {
        Variant v = new Variant(textF);
        // DataValue dv = new DataValue(v);
        DataValue dv = new DataValue(v, null, null); // >> problems in agv server
        // freeOpcUa Python
        clientTest.writeValue(node, dv);
        try {
            logger.info("{}", StatusCodes.lookup(clientTest.writeValue(node, dv).get().getValue()).get()[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeProgramNode(NodeId node, UByte textF) {
        Variant v = new Variant(textF);
        DataValue dv = new DataValue(v, null, null);
        clientTest.writeValue(node, dv);
        try {
            logger.info("{}", StatusCodes.lookup(clientTest.writeValue(node, dv).get().getValue()).get()[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeProgramNode(NodeId node, UShort textF) {
        Variant v = new Variant(textF);
        DataValue dv = new DataValue(v, null, null);
        clientTest.writeValue(node, dv);
        try {
            logger.info("{}", StatusCodes.lookup(clientTest.writeValue(node, dv).get().getValue()).get()[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeProgramNode(NodeId node, String textF) {
        Variant v = new Variant(textF);
        DataValue dv = new DataValue(v, null, null);
        clientTest.writeValue(node, dv);
    }

    public void writeProgramNode(NodeId node, float textF) {
        Variant v = new Variant(textF);
        DataValue dv = new DataValue(v, null, null);
        clientTest.writeValue(node, dv);
        try {
            logger.info("{}", StatusCodes.lookup(clientTest.writeValue(node, dv).get().getValue()).get()[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeProgramNode(NodeId node, Boolean textF) {
        Variant v = new Variant(textF);
        DataValue dv = new DataValue(v, null, null);
        clientTest.writeValue(node, dv);
        try {
            logger.info("{}", StatusCodes.lookup(clientTest.writeValue(node, dv).get().getValue()).get()[0]);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeProgramNode_AGV(NodeId node, int textF) {
        Variant v = new Variant(textF);
        DataValue dv = new DataValue(v);
        clientTest.writeValue(node, dv);
    }

    public DataValue readProgramNode(NodeId node) throws InterruptedException, ExecutionException {
        VariableNode vn = clientTest.getAddressSpace().createVariableNode(node);
        CompletableFuture<DataValue> f = vn.readValue();
        DataValue value;
        try {
            value = f.get();
        } catch (Exception e) {
            logger.error("Could not read from node {}.", node.toString(), e);
            return null;
        }
        if (value.getStatusCode().getValue() == StatusCodes.Bad_NodeIdUnknown) {
            logger.warn("Error while reading node {}:{}", node.toString(), value.getStatusCode());
        }
        return value;
    }

    public CompletableFuture<Object> callMethod(NodeId objectId, NodeId methodId, Variant[] input) {

        // NodeId objectId = new NodeId(2, "skill");
        // NodeId methodId = NodeId.parse("ns=2;s=InvokeSkill");
        // NodeId objectId = NodeId.parse("ns=2;s=/Methods");
        // NodeId methodId = NodeId.parse("ns=2;s=/Methods/invokeSkill(x)");
        CallMethodRequest request = new CallMethodRequest(objectId, methodId, input);

        return clientTest.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                Object value = result.getOutputArguments()[0].getValue();
                return CompletableFuture.completedFuture(value);
            } else {
                CompletableFuture<Object> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                System.out.println("not a result from server: " + statusCode.toString());
                return f;
            }
        });
    }

    public Map<String, NodeId> browseNode(String indent, OpcUaClient client, NodeId browseRoot) {
        Map<String, NodeId> nodes1 = null;

        try {
            List<Node> nodes = null;
            nodes = client.getAddressSpace().browse(browseRoot).get();

            nodes1 = new HashMap<String, NodeId>();
            for (int i = 0; i < nodes.size(); i++) {
                nodes1.put(nodes.get(i).getBrowseName().get().getName(), nodes.get(i).getNodeId().get());
            }
            return nodes1;
        } catch (Exception ex) {
            // REMOVED_BY_FORTISS
            // MainWindow.mainLogger.append("Erro ao tirar os nodes do
            // servidor\n");
            ex.printStackTrace();
            return nodes1;
        }

    }

    protected ArrayList<MonitoredItemCreateRequest> getMonitoredItemRequestList() {
        ArrayList<MonitoredItemCreateRequest> requestList = new ArrayList<>();

        for (int i = 0; i < readValueList.size(); i++) {
            // client handle must be unique per item
            UInteger clientHandle = uint(clientHandles.getAndIncrement());

            MonitoringParameters parameters = new MonitoringParameters(clientHandle, 100.0, // sampling
                    // interval
                    null, // filter, null means use default
                    uint(10), // queue size
                    true); // discard oldest

            ReadValueId readValueId = readValueList.get(i);
            MonitoredItemCreateRequest request = new MonitoredItemCreateRequest(readValueId, MonitoringMode.Reporting,
                    parameters);
            requestList.add(request);
        }

        return requestList;

    }

    public Integer getNsIdx(String nsUri) {
        if (nsIdxMap.containsKey(nsUri)) {
            return nsIdxMap.get(nsUri);
        }

        CompletableFuture<Integer> promiseNamespaceIdx = MsbGenericComponent.getNamespaceIndex(clientTest, nsUri);

        Integer idx;
        try {
            idx = promiseNamespaceIdx.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }

        nsIdxMap.put(nsUri, idx);

        return idx;
    }

    public abstract NodeId getNodeRun();

    public abstract NodeId getNodeReady();

    public NodeId[] getMethodNode() {
        return null;
    }

    public Variant[] getMethodArguments() {
        return null;
    }

    public Variant[] executeMethod() {
        NodeId ids[] = getMethodNode();
        if (ids == null)
            return null;
        if (ids.length != 2) {
            logger.error("Method id array length != 2");
            return null;
        }
        logger.info("Calling method {} on object {}", ids[1], ids[0]);

        CallMethodRequest request = new CallMethodRequest(ids[0], ids[1], getMethodArguments());

        try {
            CallMethodResult result = clientTest.call(request).get();

            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                logger.info("********** Method call SUCCESS **********");
                return result.getOutputArguments();
            } else {
                logger.error("###### Method call FAIL: {}", statusCode);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Could not call method", e);
            return null;
        }
    }

    public OpcUaClient getClient() {
        return clientTest;
    }
}
