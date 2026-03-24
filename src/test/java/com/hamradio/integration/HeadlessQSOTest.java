package com.hamradio.integration;

import com.hamradio.control.*;
import com.hamradio.dsp.NativeDSP;
import com.hamradio.event.EventBus;
import com.hamradio.event.events.LogEvent;
import com.hamradio.net.*;
import com.hamradio.rf.*;
import com.hamradio.rf.models.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Headless integration test: simulate a complete SSB QSO between two stations.
 * No GUI, no real hardware required.
 */
public class HeadlessQSOTest {

    public static void main(String[] args) {
        System.out.println("=== HamRadio Phase 1 — Headless QSO Test ===\n");

        EventBus eventBus = EventBus.getInstance();
        eventBus.subscribe("log", e -> {
            LogEvent le = (LogEvent) e;
            System.out.println("[" + le.getLevel() + "] " + le.getMessage());
        });

        // --- Step 1: Configure Scenario ---
        System.out.println("--- Step 1: Configure Scenario ---");
        ScenarioConfig config = new ScenarioConfig("HF SSB QSO Test");
        config.setSampleRate(44100);
        config.setDurationSeconds(10);
        config.setPropagationModel("full");

        // Station 1: Melbourne, Australia
        ScenarioConfig.StationConfig s1 = new ScenarioConfig.StationConfig(
                "VK3ABC", -37.8, 144.9, 7100000, "SSB");
        config.addStation(s1);

        // Station 2: Boston, USA
        ScenarioConfig.StationConfig s2 = new ScenarioConfig.StationConfig(
                "W1XYZ", 42.3, -71.0, 7100000, "SSB");
        config.addStation(s2);

        // --- Step 2: Validate & Start ---
        System.out.println("\n--- Step 2: Validate & Start Scenario ---");
        ResourceAllocator ra = new ResourceAllocator();
        ScenarioManager sm = new ScenarioManager(eventBus, ra);
        sm.setConfig(config);
        sm.validate();
        System.out.println("State: " + sm.getState());

        // --- Step 3: Create Stations ---
        System.out.println("\n--- Step 3: Create Stations ---");
        StationManager stationMgr = new StationManager(eventBus);
        stationMgr.createFromConfig(config);

        List<Station> stations = new ArrayList<>(stationMgr.getAllStations());
        Station stationA = stations.get(0);
        Station stationB = stations.get(1);
        double distance = stationA.distanceTo(stationB);
        System.out.println(stationA.getCallsign() + " -> " + stationB.getCallsign()
                + " distance: " + String.format("%.0f km", distance / 1000));

        // --- Step 4: Initialize DSP ---
        System.out.println("\n--- Step 4: Initialize Native DSP ---");
        NativeDSP dsp = new NativeDSP();
        int result = dsp.dspInit(config.getSampleRate());
        System.out.println("DSP init: " + (result == 0 ? "OK" : "FAILED"));

        sm.load();
        System.out.println("State: " + sm.getState());

        // --- Step 5: Generate baseband signal ---
        System.out.println("\n--- Step 5: Generate Baseband Signal ---");
        String message = "CQ CQ CQ DE VK3ABC";
        int sampleRate = config.getSampleRate();
        int samplesPerChar = sampleRate / 20;
        float[] baseband = new float[message.length() * samplesPerChar];
        for (int c = 0; c < message.length(); c++) {
            float freq = 300 + (message.charAt(c) % 64) * 30;
            float amp = (message.charAt(c) == ' ') ? 0.0f : 0.8f;
            for (int i = 0; i < samplesPerChar; i++) {
                double t = (double) i / sampleRate;
                baseband[c * samplesPerChar + i] = (float) (amp * Math.sin(2.0 * Math.PI * freq * t));
            }
        }
        System.out.println("Baseband: " + baseband.length + " samples (" +
                String.format("%.2f", (double) baseband.length / sampleRate) + " sec)");

        // --- Step 6: SSB Modulate ---
        System.out.println("\n--- Step 6: SSB Modulate (USB @ 7.1 MHz) ---");
        float carrierFreq = 7100000f;
        float[] modulated = dsp.modulateSSB(baseband, carrierFreq, sampleRate, true);
        if (modulated != null) {
            System.out.println("Modulated: " + modulated.length + " samples");
            float peak = 0;
            for (float s : modulated) if (Math.abs(s) > peak) peak = Math.abs(s);
            System.out.println("Peak amplitude: " + String.format("%.4f", peak));
        } else {
            System.out.println("ERROR: Modulation returned null!");
            return;
        }

        // --- Step 7: Apply Channel (propagation) ---
        System.out.println("\n--- Step 7: Apply RF Channel (FSPL + Multipath + Ionospheric + Noise) ---");
        RFContext rfCtx = new RFContext(carrierFreq, distance, sampleRate);

        ChannelPipeline pipeline = new ChannelPipeline();
        pipeline.addStage(new FSPLModel());
        pipeline.addStage(MultipathModel.createDefault());
        pipeline.addStage(IonosphericModel.createDefault());
        pipeline.addStage(NoiseFloorModel.createDefault());

        float[] channelOutput = pipeline.process(modulated, rfCtx);
        System.out.println("After channel: " + channelOutput.length + " samples");
        float peakAfter = 0;
        for (float s : channelOutput) if (Math.abs(s) > peakAfter) peakAfter = Math.abs(s);
        System.out.println("Peak amplitude after channel: " + String.format("%.6f", peakAfter));

        // Compute FSPL for reference
        NativeRF nativeRF = new NativeRF();
        float fsplDb = nativeRF.computeFSPL(carrierFreq, (float) distance);
        System.out.println("FSPL: " + String.format("%.1f dB", fsplDb));

        // --- Step 8: FFT Spectrum ---
        System.out.println("\n--- Step 8: FFT Spectrum Analysis ---");
        int fftSize = 1024;
        float[] fftInput = new float[fftSize];
        System.arraycopy(channelOutput, 0, fftInput, 0, Math.min(channelOutput.length, fftSize));
        float[] spectrum = dsp.fftForward(fftInput, fftSize);
        if (spectrum != null) {
            // Find peak bin
            float maxMag = 0;
            int peakBin = 0;
            for (int i = 0; i < spectrum.length / 2; i++) {
                float mag = spectrum[i * 2]; // magnitude
                if (mag > maxMag) {
                    maxMag = mag;
                    peakBin = i;
                }
            }
            System.out.println("FFT: " + (spectrum.length / 2) + " bins");
            System.out.println("Peak bin: " + peakBin + " (magnitude: " + String.format("%.6f", maxMag) + ")");
        }

        // --- Step 9: SSB Demodulate ---
        System.out.println("\n--- Step 9: SSB Demodulate ---");
        float[] demodulated = dsp.demodulateSSB(channelOutput, carrierFreq, sampleRate, true);
        if (demodulated != null) {
            System.out.println("Demodulated: " + demodulated.length + " samples");
            float peakDemod = 0;
            for (float s : demodulated) if (Math.abs(s) > peakDemod) peakDemod = Math.abs(s);
            System.out.println("Peak amplitude: " + String.format("%.6f", peakDemod));
        } else {
            System.out.println("ERROR: Demodulation returned null!");
        }

        // --- Step 10: Complete ---
        System.out.println("\n--- Step 10: Complete Scenario ---");
        sm.complete();
        dsp.dspShutdown();
        System.out.println("State: " + sm.getState());

        System.out.println("\n=== QSO Simulation Complete ===");
        System.out.println("Result: " + stationA.getCallsign() + " transmitted \"" + message
                + "\" via SSB @ 7.1 MHz over " + String.format("%.0f km", distance / 1000)
                + " to " + stationB.getCallsign());
    }
}
