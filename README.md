openMOS Workshop using Java
===========================

This repository contains the sources for the OPC UA Workshop using Java based Milo.

It contains the code for `machine` and `device`

See also the WIKI:  
https://git.fortiss.org/openmos/workshop-java/wikis/home

OPC-UA Server Information Model out of AML device description file
------------------------------------------------------------------

The [AMLParser](https://git.fortiss.org/openmos/workshop-java/blob/opcua_aml/MSB_connected_component/src/main/java/org/fortiss/uaserver/device/instance/AMLParser.java) class creates an OPC-UA server namespace based on AML file (current version is [v13](https://git.fortiss.org/openmos/workshop-java/blob/opcua_aml/MSB_connected_component/VER11.aml) ) [this companion spec](https://opcfoundation.org/news/opc-foundation-news/bridging-the-gap-between-communication-and-semantics-for-industrie-4-0-companion-specification-automationml-for-opc-ua/) . For every skill defined in aml file we create a method node in the server to trigger the corresponding low level functions in underlying devices (see _addSkill()_). Currently, the mapping between opcua/aml server and corresponding low level functions are done via string metching in _addSkill()_, where based on the skill name the corresponding *SkillMethod is instantiated. [org.fortiss.uaserver.device.lowlevel](https://git.fortiss.org/openmos/workshop-java/tree/opcua_aml/MSB_connected_component/src/main/java/org/fortiss/uaserver/device/lowlevel) package contains classes for clients, that are created by adapter to communicate with the device's servers, subscription classes for each device and skills classes that map skill methods added to the opcua/aml server in _AMLParser.addSkill()_. [ClientRunner](https://git.fortiss.org/openmos/workshop-java/blob/opcua_aml/MSB_connected_component/src/main/java/org/fortiss/uaserver/device/lowlevel/ClientRunner.java) should create a client for connecting to each of the underlying servers.

To start the server execute [org.fortiss.uaserver.device.DeviceUaServer](https://git.fortiss.org/openmos/workshop-java/blob/opcua_aml/MSB_connected_component/src/main/java/org/fortiss/uaserver/device/DeviceUaServer.java)