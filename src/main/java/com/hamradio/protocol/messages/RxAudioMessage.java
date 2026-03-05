package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class RxAudioMessage extends Message {
    private final String sourceCallsign;
    private final int sequence;
    private final float[] samples;

    public RxAudioMessage(String sourceCallsign, int sequence, float[] samples) {
        this.sourceCallsign = sourceCallsign;
        this.sequence = sequence;
        this.samples = samples.clone();
    }

    @Override
    public MessageType getType() {
        return MessageType.RX_AUDIO;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, sourceCallsign);
            dos.writeInt(sequence);
            dos.writeInt(samples.length);
            for (float sample : samples) {
                dos.writeFloat(sample);
            }
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static RxAudioMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String sourceCallsign = MessageCodec.readString(dis);
            int sequence = dis.readInt();
            int numSamples = dis.readInt();
            float[] samples = new float[numSamples];
            for (int i = 0; i < numSamples; i++) {
                samples[i] = dis.readFloat();
            }
            return new RxAudioMessage(sourceCallsign, sequence, samples);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getSourceCallsign() { return sourceCallsign; }
    public int getSequence() { return sequence; }
    public float[] getSamples() { return samples.clone(); }
}
