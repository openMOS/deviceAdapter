package org.fortiss.heartbeat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class HeartbeatClient extends Thread {

    private DatagramSocket socket;
    private InetAddress address;
    private String appUri;
    private int timeout;
    private int port;
    private boolean running;

    public HeartbeatClient(String remoteAddr, int port, String appUri, int timeout) throws SocketException, UnknownHostException {
        socket = new DatagramSocket();
        address = InetAddress.getByName(remoteAddr);
        this.port = port;
        this.appUri = appUri;
        this.timeout = timeout;
    }

    @Override
    public void run() {
        running = true;
        HeartbeatData data = new HeartbeatData();
        data.setSequence(0);
        data.setVersion((byte) 1);
        data.setNextTimeout(timeout);
        data.setAppUri(appUri);

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (running && !this.isInterrupted()) {
            long sleepUntil = (long) (System.currentTimeMillis() + timeout*0.8);

            buffer.rewind();
            int size = HeartbeatData.encode(data, buffer);

            DatagramPacket packet = new DatagramPacket(buffer.array(), size, address, port);
            try {
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }

            data.incSequence();

            try {
                Thread.sleep(Math.max(sleepUntil - System.currentTimeMillis(), 0));
            } catch (InterruptedException e) {
            }
        }

        data.setNextTimeout(0);
        int size = HeartbeatData.encode(data, buffer);
        DatagramPacket packet = new DatagramPacket(buffer.array(), size, address, port);
        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void close() {
        this.running = false;

        socket.close();
    }
}
