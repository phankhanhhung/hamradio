package com.hamradio.net;

import com.hamradio.control.ScenarioConfig;
import com.hamradio.event.EventBus;
import com.hamradio.event.events.StationEvent;

import java.util.*;

public class StationManager {

    private final Map<String, Station> stations = new LinkedHashMap<>();
    private final EventBus eventBus;

    public StationManager(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void createFromConfig(ScenarioConfig config) {
        stations.clear();
        for (ScenarioConfig.StationConfig sc : config.getStations()) {
            Station station = new Station(
                    sc.getCallsign(), sc.getLatitude(), sc.getLongitude(),
                    sc.getFrequencyHz(), sc.getMode()
            );
            station.setUpperSideband(sc.isUpperSideband());
            stations.put(sc.getCallsign(), station);
            eventBus.publish(new StationEvent(this, sc.getCallsign(), "created"));
        }
    }

    public Station getStation(String callsign) {
        return stations.get(callsign);
    }

    public Collection<Station> getAllStations() {
        return Collections.unmodifiableCollection(stations.values());
    }

    public void destroyAll() {
        for (String callsign : stations.keySet()) {
            eventBus.publish(new StationEvent(this, callsign, "destroyed"));
        }
        stations.clear();
    }
}
