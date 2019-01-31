package org.fortiss.uaserver.device.lowlevel;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

// import org.eclipse.milo.examples.server.KeyStoreLoader;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.stack.client.UaTcpStackClient;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;
import org.fortiss.uaserver.common.security.KeyStoreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientRunner
{
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CompletableFuture<OpcUaClient> future = new CompletableFuture<>();
    private final KeyStoreLoader loader = new KeyStoreLoader();
    private final String endpointUrl;
    private final Client uaClient;
    private final CompletableFuture<Boolean> futureClientReady;

    public ClientRunner(String endpointUrl, Client clientExample, CompletableFuture<Boolean> futureClientReady)
    {
        this.endpointUrl = endpointUrl;
        this.uaClient = clientExample;
        this.futureClientReady = futureClientReady;
    }

    OpcUaClient createClient() throws Exception
    {
        SecurityPolicy securityPolicy = uaClient.getSecurityPolicy();
        EndpointDescription[] endpoints = UaTcpStackClient.getEndpoints(endpointUrl).get();

        EndpointDescription endpointFinal = Arrays.stream(endpoints)
                .filter(e -> e.getSecurityPolicyUri().equals(securityPolicy.getSecurityPolicyUri()))
                .findFirst().orElseThrow(() -> new Exception("no desired endpoints returned"));

        logger.info("Using endpoint: {} [{}]", endpointFinal.getEndpointUrl(), securityPolicy);
       // REMOVED_BY_FORTISS
        // MainWindow.mainLogger.append("ClientRunner - Using endpoint: "+ endpointFinal.getEndpointUrl() + " [" + securityPolicy + "]\n" );
        loader.load();
        OpcUaClientConfig config = OpcUaClientConfig.builder()
                .setApplicationName(LocalizedText.english("fortiss opc-ua client"))
                .setApplicationUri("urn:fortiss:opcua:client")
                .setCertificate(loader.getClientCertificate())
                .setKeyPair(loader.getClientKeyPair())
                .setEndpoint(endpointFinal)
                .setIdentityProvider(uaClient.getIdentityProvider())
                .setRequestTimeout(uint(7500))
                .build();

        return new OpcUaClient(config);
    }

    public void run()
    {
        future.whenComplete((client, ex)
                ->
        {
            if (client != null)
            {
                try
                {
                    client.disconnect().get();
                    Stack.releaseSharedResources();
                } catch (InterruptedException | ExecutionException e)
                {
                    logger.error("Error disconnecting:", e.getMessage(), e);
                   // REMOVED_BY_FORTISS
                   // MainWindow.mainLogger.append("ClientRunner - Error disconnecting: " + e.getMessage()+ "\n" );
                }
            } else
            {
                logger.error("Error running example: {}", ex.getMessage(), ex);
                // REMOVED_BY_FORTISS
                // MainWindow.mainLogger.append("ClientRunner - Error running example: " + ex.getMessage() + "\n" );
                Stack.releaseSharedResources();
            }
        });

        try
        {
            OpcUaClient client = createClient();

            try
            {
                uaClient.run(client, futureClientReady);
                //future.get(5, TimeUnit.SECONDS);
            } catch (Exception t)
            {
                logger.error("Error running client example: {}", t.getMessage(), t);
                // REMOVED_BY_FORTISS
               // MainWindow.mainLogger.append("ClientRunner - Error running client example: " + t.getMessage() + "\n" );
                future.complete(client);
            }
        } catch (Exception t)
        {
            future.completeExceptionally(t);
        }
    }

}
