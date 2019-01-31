package org.fortiss.heartbeat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HeartbeatData {
    private Byte version;
    private Integer sequence;
    private Integer nextTimeout;
    private String appUri;

    public Byte getVersion() {
        return version;
    }

    public void setVersion(Byte version) {
        this.version = version;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public void incSequence() {
        this.sequence++;
    }

    public Integer getNextTimeout() {
        return nextTimeout;
    }

    public void setNextTimeout(Integer nextTimeout) {
        this.nextTimeout = nextTimeout;
    }

    public String getAppUri() {
        return appUri;
    }

    public void setAppUri(String appUri) {
        this.appUri = appUri;
    }

    public static Integer encode(HeartbeatData data, ByteBuffer bytes) {
        if (bytes.remaining() < 1)
            return 0;
        bytes.put(data.version);

        if (bytes.remaining() < 4)
            return 0;
        bytes.putInt(data.sequence);

        if (bytes.remaining() < 4)
            return 0;
        bytes.putInt(data.nextTimeout);

        if (bytes.remaining() < 4)
            return 0;
        bytes.putInt(data.appUri.length());

        if (bytes.remaining() < data.appUri.length())
            return 0;
        bytes.put(data.appUri.getBytes(StandardCharsets.US_ASCII));

        return bytes.position();
    }

    public static Integer decode(ByteBuffer bytes, HeartbeatData data) {
        if (bytes.remaining() < 1)
            return 0;
        data.version = bytes.get(0);
        bytes.position(bytes.position()+1);

        if (bytes.remaining() < 4)
            return 0;
        data.sequence = bytes.getInt();

        if (bytes.remaining() < 4)
            return 0;
        data.nextTimeout = bytes.getInt();

        if (bytes.remaining() < 4)
            return 0;
        int stringLength = bytes.getInt();

        if (bytes.remaining() < stringLength)
            return 0;
        byte[] chars = new byte[stringLength];
        bytes.get(chars, 0, stringLength);

        data.appUri = new String(chars, StandardCharsets.US_ASCII);

        return bytes.position();
    }
}
