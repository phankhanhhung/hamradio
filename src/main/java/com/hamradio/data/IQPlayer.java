package com.hamradio.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IQPlayer {

    private final String basePath;
    private DataInputStream dataStream;
    private boolean eof;

    public IQPlayer(String basePath) {
        this.basePath = basePath;
    }

    public void open() throws IOException {
        dataStream = new DataInputStream(
                new BufferedInputStream(new FileInputStream(basePath + ".sigmf-data")));
        eof = false;
    }

    public float[] read(int numSamples) throws IOException {
        if (dataStream == null || eof) return null;

        byte[] buf = new byte[numSamples * 4];
        int bytesRead = dataStream.read(buf);
        if (bytesRead <= 0) {
            eof = true;
            return null;
        }

        int samplesRead = bytesRead / 4;
        float[] samples = new float[samplesRead];
        ByteBuffer bb = ByteBuffer.wrap(buf, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < samplesRead; i++) {
            samples[i] = bb.getFloat();
        }
        return samples;
    }

    public void close() throws IOException {
        if (dataStream != null) {
            dataStream.close();
            dataStream = null;
        }
    }

    public boolean isEof() { return eof; }
}
