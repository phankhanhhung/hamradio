package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class LogMessage extends Message {
    private final String level;
    private final String message;

    public LogMessage(String level, String message) {
        this.level = level;
        this.message = message;
    }

    @Override
    public MessageType getType() {
        return MessageType.LOG;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, level);
            MessageCodec.writeString(dos, message);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static LogMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String level = MessageCodec.readString(dis);
            String message = MessageCodec.readString(dis);
            return new LogMessage(level, message);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getLevel() { return level; }
    public String getMessage() { return message; }
}
