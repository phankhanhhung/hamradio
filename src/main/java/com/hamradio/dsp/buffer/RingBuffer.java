package com.hamradio.dsp.buffer;

/**
 * Java wrapper around native SPSC ring buffer.
 */
public class RingBuffer implements AutoCloseable {
    private final NativeBuffer nativeBuffer = new NativeBuffer();
    private long handle;
    private final int capacity;

    public RingBuffer(int capacity) {
        this.capacity = capacity;
        this.handle = nativeBuffer.bufferCreate(capacity);
        if (this.handle == 0) {
            throw new RuntimeException("Failed to create native ring buffer");
        }
    }

    public int write(float[] data) {
        return write(data, 0, data.length);
    }

    public int write(float[] data, int offset, int length) {
        return nativeBuffer.bufferWrite(handle, data, offset, length);
    }

    public int read(float[] dest) {
        return read(dest, 0, dest.length);
    }

    public int read(float[] dest, int offset, int maxLength) {
        return nativeBuffer.bufferRead(handle, dest, offset, maxLength);
    }

    public int available() {
        return nativeBuffer.bufferAvailable(handle);
    }

    public void reset() {
        nativeBuffer.bufferReset(handle);
    }

    public int getCapacity() {
        return capacity;
    }

    public void close() {
        if (handle != 0) {
            nativeBuffer.bufferDestroy(handle);
            handle = 0;
        }
    }

}
