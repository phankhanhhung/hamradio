package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class TxEndMessage extends Message {
    private final long totalSamples;

    public TxEndMessage(long totalSamples) {
        this.totalSamples = totalSamples;
    }

    @Override
    public MessageType getType() {
        return MessageType.TX_END;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeLong(totalSamples);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static TxEndMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            long totalSamples = dis.readLong();
            return new TxEndMessage(totalSamples);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public long getTotalSamples() { return totalSamples; }
}
