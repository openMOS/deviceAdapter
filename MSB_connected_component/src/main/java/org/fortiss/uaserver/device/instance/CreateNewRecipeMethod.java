/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.device.instance;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.fortiss.uaserver.common.MsbGenericComponent;

public class CreateNewRecipeMethod {
    protected String addr;
    protected MsbGenericComponent msbComponent;
    // Note the number of threads should be at least the number of possible active
    // skills at the same time
    protected static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);

    public CreateNewRecipeMethod() {};

    @UaMethod
    public void invoke(AnnotationBasedInvocationHandler.InvocationContext context,
            @UaInputArgument(name = "recipe_name", description = "Recipe name") String recipeName,
            @UaInputArgument(name = "input args", description = "Input arguments") String[] inpArgs) throws UaException {
        
    }
}
