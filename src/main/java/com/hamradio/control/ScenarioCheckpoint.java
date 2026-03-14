package com.hamradio.control;

import java.io.*;
import java.util.Properties;

public class ScenarioCheckpoint {

    public static void save(ScenarioConfig config, ScenarioState state, String filePath) throws IOException {
        Properties props = new Properties();
        props.setProperty("name", config.getName());
        props.setProperty("state", state.name());
        props.setProperty("sampleRate", String.valueOf(config.getSampleRate()));
        props.setProperty("duration", String.valueOf(config.getDurationSeconds()));
        props.setProperty("propagationModel", config.getPropagationModel());
        props.setProperty("stationCount", String.valueOf(config.getStations().size()));

        for (int i = 0; i < config.getStations().size(); i++) {
            ScenarioConfig.StationConfig s = config.getStations().get(i);
            String prefix = "station." + i + ".";
            props.setProperty(prefix + "callsign", s.getCallsign());
            props.setProperty(prefix + "lat", String.valueOf(s.getLatitude()));
            props.setProperty(prefix + "lon", String.valueOf(s.getLongitude()));
            props.setProperty(prefix + "freq", String.valueOf(s.getFrequencyHz()));
            props.setProperty(prefix + "mode", s.getMode());
            props.setProperty(prefix + "usb", String.valueOf(s.isUpperSideband()));
        }

        try (OutputStream out = new FileOutputStream(filePath)) {
            props.store(out, "HamRadio Scenario Checkpoint");
        }
    }

    public static ScenarioConfig load(String filePath) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(filePath)) {
            props.load(in);
        }

        ScenarioConfig config = new ScenarioConfig(props.getProperty("name", "Loaded"));
        config.setSampleRate(Integer.parseInt(props.getProperty("sampleRate", "44100")));
        config.setDurationSeconds(Double.parseDouble(props.getProperty("duration", "60")));
        config.setPropagationModel(props.getProperty("propagationModel", "full"));

        int count = Integer.parseInt(props.getProperty("stationCount", "0"));
        for (int i = 0; i < count; i++) {
            String prefix = "station." + i + ".";
            ScenarioConfig.StationConfig s = new ScenarioConfig.StationConfig(
                    props.getProperty(prefix + "callsign", "UNKNOWN"),
                    Double.parseDouble(props.getProperty(prefix + "lat", "0")),
                    Double.parseDouble(props.getProperty(prefix + "lon", "0")),
                    Double.parseDouble(props.getProperty(prefix + "freq", "7100000")),
                    props.getProperty(prefix + "mode", "SSB")
            );
            s.setUpperSideband(Boolean.parseBoolean(props.getProperty(prefix + "usb", "true")));
            config.addStation(s);
        }

        return config;
    }

    public static ScenarioState loadState(String filePath) throws IOException {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(filePath)) {
            props.load(in);
        }
        return ScenarioState.valueOf(props.getProperty("state", "DRAFT"));
    }
}
