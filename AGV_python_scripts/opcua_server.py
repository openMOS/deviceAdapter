#!/usr/bin/env python
#import rospy
import sys
import logging
#from std_msgs.msg import String
sys.path.append('C:\Python27\Lib\site-packages\opcua')

try:
    from IPython import embed
except ImportError:
    import code

    def embed():
        vars = globals()
        vars.update(locals())
        shell = code.InteractiveConsole(vars)
        shell.interact()

from opcua import ua, uamethod, Server

pub = rospy.Publisher('/request_new_recipe', String, queue_size=10)

# method to be exposed through server
def func(parent, idRecipe):

    #msg = idRecipe.Value 
    #rospy.loginfo(idRecipe.Value)
    
    #pub.publish(msg)    
    return [ua.Variant(True, ua.VariantType.Boolean)]

# Main
# @Brief: call Construct and run the node
if __name__ == '__main__':
    
    # ROS : 
    #pub = rospy.Publisher('/request_new_recipe', String, queue_size=10)
    #rospy.init_node('teste:node')
    #rate = rospy.Rate(10) # 10hz
   ######################################################################
   # OPCUA:
    logging.basicConfig(level=logging.WARN)
   # now setup our server
    server = Server()
    #server.set_endpoint("opc.tcp://localhost:4840/freeopcua/server/")
    server.set_endpoint("opc.tcp://0.0.0.0:61499/freeopcua/server/")
    server.set_server_name("FreeOpcUa Example Server")

    # setup our own namespace
    uri = "http://examples.freeopcua.github.io"
    idx = server.register_namespace(uri)

    # get Objects node, this is where we should put our custom stuff
    objects = server.get_objects_node()

    # populating our address space
    #myfolder = objects.add_folder(idx, "myEmptyFolder")
    myobj = objects.add_object(idx, "MyObject")
    mymethod = myobj.add_method(idx, "mymethod", func, [ua.VariantType.String], [ua.VariantType.Boolean])
   ######################################################################    
    server.start()
    print("Available loggers are: ", logging.Logger.manager.loggerDict.keys())
    try:

        embed()
    finally:
        server.stop()

