package com.hamradio.net;

public class Frame {

    private final String sourceCallsign;
    private final String destCallsign;
    private final float[] payload;
    private final long timestamp;

    public Frame(String sourceCallsign, String destCallsign, float[] payload) {
        this.sourceCallsign = sourceCallsign;
        this.destCallsign = destCallsign;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSourceCallsign() { return sourceCallsign; }
    public String getDestCallsign() { return destCallsign; }
    public float[] getPayload() { return payload; }
    public long getTimestamp() { return timestamp; }
}
