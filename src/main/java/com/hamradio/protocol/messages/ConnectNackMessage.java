package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class ConnectNackMessage extends Message {
    private final String reason;

    public ConnectNackMessage(String reason) {
        this.reason = reason;
    }

    @Override
    public MessageType getType() {
        return MessageType.CONNECT_NACK;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, reason);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ConnectNackMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String reason = MessageCodec.readString(dis);
            return new ConnectNackMessage(reason);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getReason() { return reason; }
}
