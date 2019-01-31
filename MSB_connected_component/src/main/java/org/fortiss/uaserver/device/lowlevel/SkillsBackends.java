package org.fortiss.uaserver.device.lowlevel;

public enum SkillsBackends {
    LASER_SERVER_1  ("opc.tcp://200.200.200.2:21554"),
    LASER_SERVER_2  ("opc.tcp://200.200.200.3:21554"),
    LASER_SERVER_3  ("opc.tcp://200.200.200.4:21554"),
    LASER_SERVER_4  ("opc.tcp://200.200.200.5:21554"),
    PAINTING_SERVER_1  ("opc.tcp://200.200.200.6:21554"),
    PAINTING_SERVER_2  ("opc.tcp://200.200.200.7:21554"),
    PAINTING_SERVER_3  ("opc.tcp://200.200.200.8:21554"),
    GLUING_SERVER_1  ("opc.tcp://200.200.200.9:21554"),
    GLUING_SERVER_3  ("opc.tcp://200.200.200.10:21554"),
    GLUING_SERVER_2  ("opc.tcp://200.200.200.11:21554"),
    JOINING_SERVER_1  ("opc.tcp://200.200.200.12:21554"),
    JOINING_SERVER_2  ("opc.tcp://200.200.200.13:21554"),
    JOINING_SERVER_3  ("opc.tcp://200.200.200.14:21554"),
    JOINING_SERVER_4  ("opc.tcp://200.200.200.15:21554"),
    JOINING_SERVER_5  ("opc.tcp://200.200.200.16:21554"),
    JOINING_SERVER_6  ("opc.tcp://200.200.200.17:21554"),
    PRINTING_SERVER_1  ("opc.tcp://200.200.200.18:21554"),
    PRINTING_SERVER_2  ("opc.tcp://200.200.200.19:21554"),
    PRINTING_SERVER_3  ("opc.tcp://200.200.200.20:21554"),
    AGV_SERVER  ("opc.tcp://200.200.200.21:21554"),
    LABELLING_SERVER  ("opc.tcp://200.200.200.22:21554");


    private final String addr;

    private SkillsBackends(String addr) {
        this.addr = addr;
    }
    
    public String getAddr() {
        return addr;
    }
}
