package com.hamradio.protocol.messages;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;

import java.io.*;

public class RxMetadataMessage extends Message {
    private final String sourceCallsign;
    private final double snrDb;
    private final double fsplDb;
    private final double distanceMeters;
    private final int numSamples;

    public RxMetadataMessage(String sourceCallsign, double snrDb, double fsplDb,
                             double distanceMeters, int numSamples) {
        this.sourceCallsign = sourceCallsign;
        this.snrDb = snrDb;
        this.fsplDb = fsplDb;
        this.distanceMeters = distanceMeters;
        this.numSamples = numSamples;
    }

    @Override
    public MessageType getType() {
        return MessageType.RX_METADATA;
    }

    @Override
    public byte[] encodePayload() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            MessageCodec.writeString(dos, sourceCallsign);
            dos.writeDouble(snrDb);
            dos.writeDouble(fsplDb);
            dos.writeDouble(distanceMeters);
            dos.writeInt(numSamples);
            dos.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static RxMetadataMessage fromBytes(byte[] payload) {
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(payload));
            String sourceCallsign = MessageCodec.readString(dis);
            double snrDb = dis.readDouble();
            double fsplDb = dis.readDouble();
            double distanceMeters = dis.readDouble();
            int numSamples = dis.readInt();
            return new RxMetadataMessage(sourceCallsign, snrDb, fsplDb, distanceMeters, numSamples);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String getSourceCallsign() { return sourceCallsign; }
    public double getSnrDb() { return snrDb; }
    public double getFsplDb() { return fsplDb; }
    public double getDistanceMeters() { return distanceMeters; }
    public int getNumSamples() { return numSamples; }
}
