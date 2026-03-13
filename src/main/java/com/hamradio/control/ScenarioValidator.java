package com.hamradio.control;

import java.util.ArrayList;
import java.util.List;

public class ScenarioValidator {

    public List<String> validate(ScenarioConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getName() == null || config.getName().trim().isEmpty()) {
            errors.add("Scenario name is required");
        }
        if (config.getStations().size() < 2) {
            errors.add("At least 2 stations are required");
        }
        if (config.getSampleRate() <= 0) {
            errors.add("Sample rate must be positive");
        }
        if (config.getDurationSeconds() <= 0) {
            errors.add("Duration must be positive");
        }

        for (int i = 0; i < config.getStations().size(); i++) {
            ScenarioConfig.StationConfig s = config.getStations().get(i);
            if (s.getCallsign() == null || s.getCallsign().trim().isEmpty()) {
                errors.add("Station " + i + ": callsign is required");
            }
            if (s.getFrequencyHz() <= 0) {
                errors.add("Station " + i + ": frequency must be positive");
            }
            String mode = s.getMode();
            if (mode == null || (!mode.equals("AM") && !mode.equals("FM") && !mode.equals("SSB"))) {
                errors.add("Station " + i + ": mode must be AM, FM, or SSB");
            }
        }

        return errors;
    }
}
