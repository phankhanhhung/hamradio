package com.hamradio.event.events;

import com.hamradio.event.Event;

public class SpectrumDataEvent extends Event {
    private final float[] magnitudes;
    private final String stationId;

    public SpectrumDataEvent(Object source, float[] magnitudes, String stationId) {
        super("spectrum.data", source);
        this.magnitudes = magnitudes;
        this.stationId = stationId;
    }

    public float[] getMagnitudes() { return magnitudes; }
    public String getStationId() { return stationId; }
}
