package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class StationUpdateMessage extends Message {
    private final String callsign;
    private final String state;
    private final double frequencyHz;

    public StationUpdateMessage(String callsign, String state, double frequencyHz) {
        this.callsign = callsign;
        this.state = state;
        this.frequencyHz = frequencyHz;
    }

    @Override
    public MessageType getType() {
        return MessageType.STATION_UPDATE;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, callsign);
            MessageCodec.writeString(dos, state);
            dos.writeDouble(frequencyHz);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static StationUpdateMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String callsign = MessageCodec.readString(dis);
            String state = MessageCodec.readString(dis);
            double frequencyHz = dis.readDouble();
            return new StationUpdateMessage(callsign, state, frequencyHz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getCallsign() { return callsign; }
    public String getState() { return state; }
    public double getFrequencyHz() { return frequencyHz; }
}
