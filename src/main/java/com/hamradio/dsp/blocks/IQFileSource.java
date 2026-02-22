package com.hamradio.dsp.blocks;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IQFileSource extends SourceBlock {

    private final String filePath;
    private DataInputStream stream;
    private boolean eof;

    public IQFileSource(String id, String filePath) {
        super(id);
        this.filePath = filePath;
    }

    @Override
    public void initialize() {
        try {
            stream = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)));
            eof = false;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("IQ file not found: " + filePath, e);
        }
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        if (eof || stream == null) {
            java.util.Arrays.fill(output, 0, numSamples, 0.0f);
            return;
        }
        byte[] buf = new byte[numSamples * 4];
        try {
            int bytesRead = stream.read(buf);
            if (bytesRead <= 0) {
                eof = true;
                java.util.Arrays.fill(output, 0, numSamples, 0.0f);
                return;
            }
            ByteBuffer bb = ByteBuffer.wrap(buf, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN);
            int samplesRead = bytesRead / 4;
            for (int i = 0; i < samplesRead; i++) {
                output[i] = bb.getFloat();
            }
            for (int i = samplesRead; i < numSamples; i++) {
                output[i] = 0.0f;
            }
        } catch (IOException e) {
            eof = true;
        }
    }

    @Override
    public int getOutputSize(int inputSize) {
        return inputSize;
    }

    @Override
    public void dispose() {
        if (stream != null) {
            try { stream.close(); } catch (IOException ignored) { }
        }
    }

    public boolean isEof() { return eof; }
}
