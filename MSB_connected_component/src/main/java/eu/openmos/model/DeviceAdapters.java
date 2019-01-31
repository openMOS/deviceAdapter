package eu.openmos.model;

public enum DeviceAdapters {
	IntrosysKepware("opc.tcp://172.18.3.80:49320"), IntrosysMatricon(
			"opc.tcp://172.20.11.100:21381/MatriconOpcUaWrapper"), IntrosysGazebo("opc.tcp://172.18.3.51:16665");

	private String addr;

	DeviceAdapters(String s) {
		addr = s;
	}

	public String getAddress() {
		return addr;
	}
}