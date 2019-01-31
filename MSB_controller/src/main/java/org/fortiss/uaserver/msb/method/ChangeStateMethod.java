/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.msb.method;

import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.annotations.UaOutputArgument;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.Out;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeStateMethod {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	@UaMethod
	public void invoke(InvocationContext context,

			@UaInputArgument(name = "da_id", description = "device adapter id") String da_id,

			@UaInputArgument(name = "recipe_id", description = "recipe id") String recipe_id,

			@UaInputArgument(name = "product_id", description = "product id") String product_id,

			@UaOutputArgument(name = "res", description = "feedback") Out<String> res) {

		logger.info("changeState got da followin'\n\tda_id: " + da_id + "\n\trecipe_id: " + recipe_id + "\n\tproduct_id: "
				+ product_id + "\n");

		res.set("got it!");
	}

}
