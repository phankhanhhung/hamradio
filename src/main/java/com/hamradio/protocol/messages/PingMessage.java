package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class PingMessage extends Message {
    private final long timestamp;

    public PingMessage(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public MessageType getType() {
        return MessageType.PING;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeLong(timestamp);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static PingMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            long timestamp = dis.readLong();
            return new PingMessage(timestamp);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public long getTimestamp() { return timestamp; }
}
