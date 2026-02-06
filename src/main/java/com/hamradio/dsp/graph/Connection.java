package com.hamradio.dsp.graph;

import com.hamradio.dsp.buffer.RingBuffer;

public class Connection {

    private static final int DEFAULT_BUFFER_SIZE = 65536;

    private final Port source;
    private final Port destination;
    private final RingBuffer buffer;

    public Connection(Port source, Port destination) {
        this(source, destination, DEFAULT_BUFFER_SIZE);
    }

    public Connection(Port source, Port destination, int bufferSize) {
        this.source = source;
        this.destination = destination;
        this.buffer = new RingBuffer(bufferSize);
    }

    public Port getSource() { return source; }
    public Port getDestination() { return destination; }
    public RingBuffer getBuffer() { return buffer; }

    public void close() {
        buffer.close();
    }
}
