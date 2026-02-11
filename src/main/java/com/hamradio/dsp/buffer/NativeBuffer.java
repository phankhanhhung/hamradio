package com.hamradio.dsp.buffer;

public class NativeBuffer {

    static {
        System.loadLibrary("hamradio");
    }

    public native long bufferCreate(int capacitySamples);
    public native int bufferWrite(long handle, float[] data, int offset, int length);
    public native int bufferRead(long handle, float[] dest, int offset, int maxLength);
    public native int bufferAvailable(long handle);
    public native void bufferReset(long handle);
    public native void bufferDestroy(long handle);
}
