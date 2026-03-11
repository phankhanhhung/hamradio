package com.hamradio.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScenarioConfig {

    public static class StationConfig {
        private String callsign;
        private double latitude;
        private double longitude;
        private double frequencyHz;
        private String mode; // AM, FM, SSB
        private boolean upperSideband; // for SSB

        public StationConfig(String callsign, double lat, double lon, double freqHz, String mode) {
            this.callsign = callsign;
            this.latitude = lat;
            this.longitude = lon;
            this.frequencyHz = freqHz;
            this.mode = mode;
            this.upperSideband = true;
        }

        public String getCallsign() { return callsign; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public double getFrequencyHz() { return frequencyHz; }
        public String getMode() { return mode; }
        public boolean isUpperSideband() { return upperSideband; }
        public void setUpperSideband(boolean usb) { this.upperSideband = usb; }
    }

    private String name;
    private List<StationConfig> stations = new ArrayList<>();
    private String propagationModel = "fspl";
    private int sampleRate = 44100;
    private double durationSeconds = 60.0;
    private Map<String, String> parameters = new HashMap<>();

    public ScenarioConfig(String name) {
        this.name = name;
    }

    public void addStation(StationConfig station) {
        stations.add(station);
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<StationConfig> getStations() { return stations; }
    public String getPropagationModel() { return propagationModel; }
    public void setPropagationModel(String model) { this.propagationModel = model; }
    public int getSampleRate() { return sampleRate; }
    public void setSampleRate(int rate) { this.sampleRate = rate; }
    public double getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(double dur) { this.durationSeconds = dur; }
    public Map<String, String> getParameters() { return parameters; }
    public void setParameter(String key, String value) { parameters.put(key, value); }
}
