package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class TxAudioMessage extends Message {
    private final int sequence;
    private final float[] samples;

    public TxAudioMessage(int sequence, float[] samples) {
        this.sequence = sequence;
        this.samples = samples.clone();
    }

    @Override
    public MessageType getType() {
        return MessageType.TX_AUDIO;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
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

    public static TxAudioMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            int sequence = dis.readInt();
            int numSamples = dis.readInt();
            float[] samples = new float[numSamples];
            for (int i = 0; i < numSamples; i++) {
                samples[i] = dis.readFloat();
            }
            return new TxAudioMessage(sequence, samples);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int getSequence() { return sequence; }
    public float[] getSamples() { return samples.clone(); }
}
