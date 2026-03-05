package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class TuneMessage extends Message {
    private final double frequencyHz;
    private final String mode;
    private final boolean upperSideband;

    public TuneMessage(double frequencyHz, String mode, boolean upperSideband) {
        this.frequencyHz = frequencyHz;
        this.mode = mode;
        this.upperSideband = upperSideband;
    }

    @Override
    public MessageType getType() {
        return MessageType.TUNE;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeDouble(frequencyHz);
            MessageCodec.writeString(dos, mode);
            dos.writeByte(upperSideband ? 1 : 0);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TuneMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            double frequencyHz = dis.readDouble();
            String mode = MessageCodec.readString(dis);
            boolean upperSideband = dis.readByte() != 0;
            return new TuneMessage(frequencyHz, mode, upperSideband);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public double getFrequencyHz() { return frequencyHz; }
    public String getMode() { return mode; }
    public boolean isUpperSideband() { return upperSideband; }
}
