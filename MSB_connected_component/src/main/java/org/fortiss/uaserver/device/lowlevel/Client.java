package org.fortiss.uaserver.device.lowlevel;

import java.util.concurrent.CompletableFuture;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;


public interface Client {

    default SecurityPolicy getSecurityPolicy() 
    {
        return SecurityPolicy.None;
    }

    default IdentityProvider getIdentityProvider() 
    {
        return new AnonymousProvider();
    }

    void run(OpcUaClient client, CompletableFuture<Boolean> futureClientReady) throws Exception;

}
