package com.hamradio.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IQRecorder {

    private final String basePath;
    private final SigMFMetadata metadata;
    private DataOutputStream dataStream;
    private long samplesWritten;

    public IQRecorder(String basePath, double sampleRate, double frequency) {
        this.basePath = basePath;
        this.metadata = new SigMFMetadata(sampleRate, frequency);
        this.samplesWritten = 0;
    }

    public void start() throws IOException {
        dataStream = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(basePath + ".sigmf-data")));
        metadata.addCapture(0, metadata.getFrequency());
    }

    public void write(float[] samples) throws IOException {
        if (dataStream == null) return;
        ByteBuffer bb = ByteBuffer.allocate(samples.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float s : samples) {
            bb.putFloat(s);
        }
        dataStream.write(bb.array());
        samplesWritten += samples.length;
    }

    public void stop() throws IOException {
        if (dataStream != null) {
            dataStream.close();
            dataStream = null;
        }
        metadata.save(basePath + ".sigmf-meta");
    }

    public long getSamplesWritten() { return samplesWritten; }
    public SigMFMetadata getMetadata() { return metadata; }
}
