package org.fortiss.heartbeat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatServer extends Thread {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private DatagramSocket socket;
    private boolean running;
    private byte[] buf = new byte[1024];
    private TimeoutChecker timeoutChecker;

    private Consumer<String> timeoutNotifier;

    private class ClientTimeoutData {
        public Instant timeoutAt;
    }

    private final Map<String, ClientTimeoutData> clientTimeouts;

    public HeartbeatServer(int port, Consumer<String> timeoutNotifier) throws SocketException {
        socket = new DatagramSocket(port);
        clientTimeouts = new HashMap<>();
        this.timeoutNotifier = timeoutNotifier;


    }

    public void run() {
        this.running = true;
        timeoutChecker = new TimeoutChecker();
        timeoutChecker.start();

        while (running && !this.isInterrupted()) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(packet);


                ByteBuffer buffer = ByteBuffer.allocate(packet.getLength());
                buffer.put(packet.getData(), 0, packet.getLength());

                HeartbeatData data = new HeartbeatData();
                buffer.rewind();
                while (HeartbeatData.decode(buffer, data) > 0) {
                    processData(data);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }

    private void processData(HeartbeatData data) {
        /*System.out.println("Got data: \n\tVersion: " + data.getVersion()
            +" \n\tSequence: "+ data.getSequence()
            +" \n\tID: " + data.getAppUri()
            + "\n\tTimeout: " + data.getNextTimeout());*/


        synchronized (clientTimeouts) {
            if (data.getNextTimeout() == 0) {
                if (clientTimeouts.containsKey(data.getAppUri())) {
                    clientTimeouts.remove(data.getAppUri());
                }
                ClientTimeoutData timoutData = new ClientTimeoutData();
                timoutData.timeoutAt = Instant.now();
                notifyTimeout(data.getAppUri(), timoutData);
                return;
            }

            if (clientTimeouts.containsKey(data.getAppUri())) {
                clientTimeouts.get(data.getAppUri()).timeoutAt =
                    Instant.now().plus(data.getNextTimeout(), ChronoUnit.MILLIS);
            } else {
                ClientTimeoutData timoutData = new ClientTimeoutData();
                timoutData.timeoutAt = Instant.now().plus(data.getNextTimeout(), ChronoUnit.MILLIS);
                clientTimeouts.put(data.getAppUri(), timoutData);
            }
        }
    }


    public void unregister(String serverUri) {
        synchronized (clientTimeouts) {
            if (clientTimeouts.containsKey(serverUri)) {
                clientTimeouts.remove(serverUri);
            }
        }
    }


    private void notifyTimeout(String appUri, ClientTimeoutData data) {
        logger.info("Heartbeat of {} timed out", appUri);
        if (timeoutNotifier != null) {
            timeoutNotifier.accept(appUri);
        }
    }

    public void shutdown() {
        this.running = false;
        this.interrupt();
    }

    private class TimeoutChecker extends Thread {
        public void run() {
            while (running && !this.isInterrupted()) {
                synchronized (clientTimeouts) {
                    for (String s : clientTimeouts.keySet()) {
                        if (Instant.now().isAfter(clientTimeouts.get(s).timeoutAt)) {
                            clientTimeouts.remove(s);
                            notifyTimeout(s, clientTimeouts.get(s));
                        }
                    }
                }
                try {
                    sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
