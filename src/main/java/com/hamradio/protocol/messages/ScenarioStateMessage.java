package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class ScenarioStateMessage extends Message {
    private final String oldState;
    private final String newState;

    public ScenarioStateMessage(String oldState, String newState) {
        this.oldState = oldState;
        this.newState = newState;
    }

    @Override
    public MessageType getType() {
        return MessageType.SCENARIO_STATE;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, oldState);
            MessageCodec.writeString(dos, newState);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ScenarioStateMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String oldState = MessageCodec.readString(dis);
            String newState = MessageCodec.readString(dis);
            return new ScenarioStateMessage(oldState, newState);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getOldState() { return oldState; }
    public String getNewState() { return newState; }
}
