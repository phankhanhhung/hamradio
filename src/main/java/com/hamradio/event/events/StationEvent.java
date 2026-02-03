package com.hamradio.event.events;

import com.hamradio.event.Event;

public class StationEvent extends Event {
    private final String stationId;
    private final String action;

    public StationEvent(Object source, String stationId, String action) {
        super("station", source);
        this.stationId = stationId;
        this.action = action;
    }

    public String getStationId() { return stationId; }
    public String getAction() { return action; }
}
