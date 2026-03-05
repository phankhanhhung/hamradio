package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class TxBeginMessage extends Message {
    private final int sampleRate;

    public TxBeginMessage(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    @Override
    public MessageType getType() {
        return MessageType.TX_BEGIN;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(sampleRate);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TxBeginMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            int sampleRate = dis.readInt();
            return new TxBeginMessage(sampleRate);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public int getSampleRate() { return sampleRate; }
}
