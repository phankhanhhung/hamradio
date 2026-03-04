package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class ConnectMessage extends Message {
    private final String callsign;
    private final double latitude;
    private final double longitude;
    private final double frequencyHz;
    private final String mode;
    private final boolean upperSideband;

    public ConnectMessage(String callsign, double latitude, double longitude,
                          double frequencyHz, String mode, boolean upperSideband) {
        this.callsign = callsign;
        this.latitude = latitude;
        this.longitude = longitude;
        this.frequencyHz = frequencyHz;
        this.mode = mode;
        this.upperSideband = upperSideband;
    }

    @Override
    public MessageType getType() {
        return MessageType.CONNECT;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, callsign);
            dos.writeDouble(latitude);
            dos.writeDouble(longitude);
            dos.writeDouble(frequencyHz);
            MessageCodec.writeString(dos, mode);
            dos.writeByte(upperSideband ? 1 : 0);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ConnectMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String callsign = MessageCodec.readString(dis);
            double latitude = dis.readDouble();
            double longitude = dis.readDouble();
            double frequencyHz = dis.readDouble();
            String mode = MessageCodec.readString(dis);
            boolean upperSideband = dis.readByte() != 0;
            return new ConnectMessage(callsign, latitude, longitude, frequencyHz, mode, upperSideband);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getCallsign() { return callsign; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getFrequencyHz() { return frequencyHz; }
    public String getMode() { return mode; }
    public boolean isUpperSideband() { return upperSideband; }
}
