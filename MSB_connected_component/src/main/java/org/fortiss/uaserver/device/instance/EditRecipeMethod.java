package org.fortiss.uaserver.device.instance;

import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.UaException;

public class EditRecipeMethod {
    @UaMethod
    public void invoke(AnnotationBasedInvocationHandler.InvocationContext context,
            @UaInputArgument(name = "recipe", description = "Recipe") String recipe) throws UaException {
        new AMLWriter(recipe);
    }
}
