package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConnectAckMessage extends Message {
    private final String sessionId;
    private final int sampleRate;
    private final String propagationModel;
    private final List<String> stationCallsigns;

    public ConnectAckMessage(String sessionId, int sampleRate, String propagationModel,
                             List<String> stationCallsigns) {
        this.sessionId = sessionId;
        this.sampleRate = sampleRate;
        this.propagationModel = propagationModel;
        this.stationCallsigns = new ArrayList<>(stationCallsigns);
    }

    @Override
    public MessageType getType() {
        return MessageType.CONNECT_ACK;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, sessionId);
            dos.writeInt(sampleRate);
            MessageCodec.writeString(dos, propagationModel);
            dos.writeInt(stationCallsigns.size());
            for (String callsign : stationCallsigns) {
                MessageCodec.writeString(dos, callsign);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ConnectAckMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String sessionId = MessageCodec.readString(dis);
            int sampleRate = dis.readInt();
            String propagationModel = MessageCodec.readString(dis);
            int count = dis.readInt();
            List<String> stationCallsigns = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                stationCallsigns.add(MessageCodec.readString(dis));
            }
            return new ConnectAckMessage(sessionId, sampleRate, propagationModel, stationCallsigns);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getSessionId() { return sessionId; }
    public int getSampleRate() { return sampleRate; }
    public String getPropagationModel() { return propagationModel; }
    public List<String> getStationCallsigns() { return stationCallsigns; }
}
