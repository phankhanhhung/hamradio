package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class SpectrumDataMessage extends Message {
    private final String stationId;
    private final float[] magnitudes;

    public SpectrumDataMessage(String stationId, float[] magnitudes) {
        this.stationId = stationId;
        this.magnitudes = magnitudes.clone();
    }

    @Override
    public MessageType getType() {
        return MessageType.SPECTRUM_DATA;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, stationId);
            dos.writeInt(magnitudes.length);
            for (float mag : magnitudes) {
                dos.writeFloat(mag);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static SpectrumDataMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String stationId = MessageCodec.readString(dis);
            int numBins = dis.readInt();
            float[] magnitudes = new float[numBins];
            for (int i = 0; i < numBins; i++) {
                magnitudes[i] = dis.readFloat();
            }
            return new SpectrumDataMessage(stationId, magnitudes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getStationId() { return stationId; }
    public float[] getMagnitudes() { return magnitudes.clone(); }
}
