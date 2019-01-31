/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.fortiss.uaserver.machine.object;

import java.util.Optional;

import org.eclipse.milo.opcua.sdk.core.model.BasicProperty;
import org.eclipse.milo.opcua.sdk.core.model.Property;
import org.eclipse.milo.opcua.sdk.server.api.ServerNodeMap;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.PropertyNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;

/**
 *
 * @author cheng
 */
public class SkillTypeNode extends UaObjectTypeNode {
    
 //   UaVariableTypeNode motorStatus;
 //   static int TO_BE_CHANGED = 100;
 //   static int RANK = -2;
    
   public Property<UByte> SERVICE_LEVEL = new BasicProperty<>(
        QualifiedName.parse("0:ServiceLevel"),
        NodeId.parse("ns=0;i=3"),
        -1,
        UByte.class
    );
    

    public UByte getServiceLevel() {
        Optional<UByte> property = getProperty(SERVICE_LEVEL);

        return property.orElse(null);
    }

   
    public PropertyNode getServiceLevelNode() {
        Optional<VariableNode> propertyNode = getPropertyNode(SERVICE_LEVEL.getBrowseName());

        return propertyNode.map(n -> (PropertyNode) n).orElse(null);
    }


    public void setServiceLevel(UByte value) {
        setProperty(SERVICE_LEVEL, value);
    }
    
    public SkillTypeNode(ServerNodeMap nodeMap, NodeId nodeId, QualifiedName browseName, LocalizedText displayName, 
            LocalizedText description, UInteger writeMask, UInteger userWriteMask, boolean isAbstract ) {
       //   ,  OpcUaServer server, UShort namespaceIndex) {
    	super(nodeMap, nodeId, browseName, displayName, description, writeMask, userWriteMask, isAbstract);
      
      /*       
   NodeId serviceLevelNodeId = new NodeId(namespaceIndex, "ServiceLevel");
       PropertyNode propNode = getServiceLevelNode();
        
       
       
          try {
            server.getUaNamespace().addReference(
                    nodeId,
                    Identifiers.HasProperty,
                    true,
                    serviceLevelNodeId.expanded(),
                    NodeClass.ObjectType
            );
        } catch (UaException e) {
            System.out.println("Error adding reference to Object type folder: " + e.getMessage());
        }
       
        */
        
        /*
        NodeId motorStatusNodeId = new NodeId(namespaceIndex, "MotorType");
        NodeId motorStatusTypeId = (NodeId) Identifiers.Boolean;
        Variant variant = new Variant(false);
         
       motorStatus =  new UaVariableTypeNode( nodeManager,
                              motorStatusNodeId,
                              new QualifiedName(0, "Status"),
                              new LocalizedText("en", "Status"),
                              Optional.of(new LocalizedText("en", "Status of the motor")),
                             Optional.of(UInteger.valueOf(0L)), 
                             Optional.of(UInteger.valueOf(0L)),
                            Optional.of(new DataValue(variant)),
                              motorStatusTypeId,
                              RANK, Optional.of(new UInteger[]{}),
                              false);
        
  


        nodeManager.put(motorStatusNodeId, motorStatus);
        
          try {
            server.getUaNamespace().addReference(
                    nodeId,
                    Identifiers.Organizes,
                    true,
                    motorStatusNodeId.expanded(),
                    NodeClass.ObjectType
            );
        } catch (UaException e) {
            System.out.println("Error adding reference to Object type folder: " + e.getMessage());
        }
     
*/
    }
    
  
}
