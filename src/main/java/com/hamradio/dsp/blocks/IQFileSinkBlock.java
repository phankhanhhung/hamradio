package com.hamradio.dsp.blocks;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IQFileSinkBlock extends SinkBlock {

    private final String filePath;
    private DataOutputStream stream;
    private long samplesWritten;

    public IQFileSinkBlock(String id, String filePath) {
        super(id);
        this.filePath = filePath;
    }

    @Override
    public void initialize() {
        try {
            stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filePath)));
            samplesWritten = 0;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Cannot create IQ file: " + filePath, e);
        }
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        if (stream == null || input == null) return;
        try {
            ByteBuffer bb = ByteBuffer.allocate(numSamples * 4).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < numSamples; i++) {
                bb.putFloat(input[i]);
            }
            stream.write(bb.array());
            samplesWritten += numSamples;
        } catch (IOException e) {
            System.err.println("[IQFileSink] Write error: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        if (stream != null) {
            try { stream.close(); } catch (IOException ignored) { }
        }
    }

    public long getSamplesWritten() { return samplesWritten; }
    public String getFilePath() { return filePath; }
}
