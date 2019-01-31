#!/usr/bin/env python
import rospy
import sys
import logging
from std_msgs.msg import String
sys.path.append('/home/jose/python-opcua')

try:
    from IPython import embed
except ImportError:
    import code

    def embed():
        vars = globals()
        vars.update(locals())
        shell = code.InteractiveConsole(vars)
        shell.interact()

from opcua import ua, uamethod, Client



if __name__ == "__main__":
    logging.basicConfig(level=logging.WARN)

    client = Client("opc.tcp://172.18.3.51:4840/freeopcua/server/")
    
    
    try:
        client.connect()

        # Client has a few methods to get proxy to UA nodes that should always be in address space such as Root or Objects
        root = client.get_root_node()
        print("Root node is: ", root)
        objects = client.get_objects_node()
        print("Objects node is: ", objects)

        obj = root.get_child(["0:Objects", "2:MyObject"])

        res = obj.call_method("2:mymethod",sys.argv[1])
        print("method result is: ", res)

        embed()
    finally:
        client.disconnect()
