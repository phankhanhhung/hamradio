package com.hamradio.server;

import com.hamradio.dsp.NativeDSP;
import com.hamradio.net.Station;
import com.hamradio.protocol.messages.RxAudioMessage;
import com.hamradio.protocol.messages.RxMetadataMessage;
import com.hamradio.protocol.messages.SpectrumDataMessage;
import com.hamradio.rf.ChannelPipeline;
import com.hamradio.rf.NativeRF;
import com.hamradio.rf.RFContext;
import com.hamradio.rf.models.FSPLModel;
import com.hamradio.rf.models.IonosphericModel;
import com.hamradio.rf.models.MultipathModel;
import com.hamradio.rf.models.NoiseFloorModel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core simulation engine for the server.
 * <p>
 * When a client transmits audio, the engine:
 *  1. Modulates the baseband signal according to the TX station's mode.
 *  2. Computes a TX spectrum and sends it back to the transmitter.
 *  3. For every OTHER connected client, applies the full RF channel model
 *     (FSPL, multipath, ionospheric, noise) based on distance and frequency,
 *     demodulates the result, estimates SNR, and delivers RxAudioMessage,
 *     SpectrumDataMessage, and RxMetadataMessage to the receiver.
 */
public class SimulationEngine {

    private final NativeDSP nativeDSP = new NativeDSP();
    private final NativeRF nativeRF = new NativeRF();
    private final ClientRegistry registry;
    private final String propagationModel;
    private final int sampleRate;
    private final AtomicInteger sequenceCounter = new AtomicInteger(0);

    private static final int FFT_SIZE = 1024;
    private static final float FM_DEVIATION = 5000f;

    public SimulationEngine(ClientRegistry registry, String propagationModel, int sampleRate) {
        this.registry = registry;
        this.propagationModel = propagationModel;
        this.sampleRate = sampleRate;
        nativeDSP.dspInit(sampleRate);
    }

    /**
     * Processes a TX audio buffer from one client and delivers the simulated
     * received signal to every other client in the registry.
     *
     * @param txCallsign the callsign of the transmitting station
     * @param samples    raw baseband audio samples from the transmitter
     */
    public void processTxAudio(String txCallsign, float[] samples) {
        if (samples == null || samples.length == 0) {
            return;
        }

        ClientHandler txHandler = registry.getHandler(txCallsign);
        if (txHandler == null || txHandler.getStation() == null) {
            System.err.println("[SimEngine] Unknown TX callsign: " + txCallsign);
            return;
        }

        Station txStation = txHandler.getStation();
        float txFreq = (float) txStation.getFrequencyHz();
        String txMode = txStation.getMode();
        boolean txUsb = txStation.isUpperSideband();

        // Step 1: Modulate baseband → RF signal
        float[] modulated = modulate(samples, txMode, txFreq, sampleRate, txUsb);
        if (modulated == null) {
            System.err.println("[SimEngine] Modulation failed for " + txCallsign);
            return;
        }

        // Step 2: Compute TX spectrum and send back to transmitter
        float[] txSpectrum = computeSpectrum(modulated);
        if (txSpectrum != null) {
            txHandler.sendMessage(new SpectrumDataMessage(txCallsign, txSpectrum));
        }

        // Step 3: For each OTHER client, simulate channel and deliver
        for (ClientHandler rxHandler : registry.getAllHandlers()) {
            if (rxHandler.getCallsign() == null
                    || rxHandler.getCallsign().equals(txCallsign)
                    || rxHandler.getStation() == null) {
                continue;
            }

            try {
                deliverToReceiver(txStation, txCallsign, modulated, txFreq, rxHandler);
            } catch (Exception e) {
                System.err.println("[SimEngine] Error delivering to "
                        + rxHandler.getCallsign() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Applies channel model between TX and one RX station, then sends audio,
     * spectrum, and metadata messages to the receiver.
     */
    private void deliverToReceiver(Station txStation, String txCallsign,
                                   float[] modulated, float txFreq,
                                   ClientHandler rxHandler) {
        Station rxStation = rxHandler.getStation();
        String rxMode = rxStation.getMode();
        boolean rxUsb = rxStation.isUpperSideband();
        float rxFreq = (float) rxStation.getFrequencyHz();

        // (a) Compute distance
        double distance = txStation.distanceTo(rxStation);

        // (b) Create RF context
        RFContext rfCtx = new RFContext(txFreq, distance, sampleRate);

        // (c) Build channel pipeline
        ChannelPipeline pipeline = buildPipeline(propagationModel);

        // (d) Apply pipeline to get the degraded signal
        float[] channelOutput = pipeline.process(modulated, rfCtx);

        // (e) Compute RX spectrum (FFT of received signal)
        float[] rxSpectrum = computeSpectrum(channelOutput);

        // (f) Compute SNR
        double rxPower = computePower(channelOutput);
        double fsplDb = computeFSPLdB(txFreq, distance);
        double txPower = computePower(modulated);
        double expectedSignalPower = txPower * Math.pow(10, -fsplDb / 10);
        double noisePower = Math.max(rxPower - expectedSignalPower, 1e-20);
        double estimatedSnr = Math.max(-30, Math.min(60,
                10 * Math.log10(expectedSignalPower / noisePower)));

        // (g) Demodulate using the RECEIVER's mode/frequency
        float[] demodulated = demodulate(channelOutput, rxMode, rxFreq, sampleRate, rxUsb);

        int seq = sequenceCounter.getAndIncrement();

        // (h) Send RxAudioMessage with demodulated audio
        if (demodulated != null) {
            rxHandler.sendMessage(new RxAudioMessage(txCallsign, seq, demodulated));
        }

        // (i) Send SpectrumDataMessage for the received signal
        if (rxSpectrum != null) {
            rxHandler.sendMessage(new SpectrumDataMessage(txCallsign, rxSpectrum));
        }

        // (j) Send RxMetadataMessage with signal quality info
        float fsplActual = nativeRF.computeFSPL(txFreq, (float) distance);
        int numDemodSamples = (demodulated != null) ? demodulated.length : 0;
        rxHandler.sendMessage(new RxMetadataMessage(
                txCallsign, estimatedSnr, fsplActual, distance, numDemodSamples));
    }

    /**
     * Builds a ChannelPipeline matching the selected propagation model.
     * Mirrors the logic in MainWindow.buildPipeline.
     */
    private ChannelPipeline buildPipeline(String model) {
        ChannelPipeline pipeline = new ChannelPipeline();
        switch (model) {
            case "fspl":
                pipeline.addStage(new FSPLModel());
                break;
            case "multipath":
                pipeline.addStage(new FSPLModel());
                pipeline.addStage(MultipathModel.createDefault());
                break;
            case "ionospheric":
                pipeline.addStage(new FSPLModel());
                pipeline.addStage(IonosphericModel.createDefault());
                break;
            case "full":
            default:
                pipeline.addStage(new FSPLModel());
                pipeline.addStage(MultipathModel.createDefault());
                pipeline.addStage(IonosphericModel.createDefault());
                pipeline.addStage(NoiseFloorModel.createDefault());
                break;
        }
        return pipeline;
    }

    /**
     * Modulates baseband audio into an RF signal for the given mode.
     */
    private float[] modulate(float[] baseband, String mode, float freq,
                             int sampleRate, boolean upperSideband) {
        switch (mode) {
            case "AM":
                return nativeDSP.modulateAM(baseband, freq, sampleRate);
            case "FM":
                return nativeDSP.modulateFM(baseband, freq, FM_DEVIATION, sampleRate);
            case "SSB":
            default:
                return nativeDSP.modulateSSB(baseband, freq, sampleRate, upperSideband);
        }
    }

    /**
     * Demodulates an RF signal back to baseband audio for the given mode.
     */
    private float[] demodulate(float[] signal, String mode, float freq,
                               int sampleRate, boolean upperSideband) {
        switch (mode) {
            case "AM":
                return nativeDSP.demodulateAM(signal, freq, sampleRate);
            case "FM":
                return nativeDSP.demodulateFM(signal, freq, FM_DEVIATION, sampleRate);
            case "SSB":
            default:
                return nativeDSP.demodulateSSB(signal, freq, sampleRate, upperSideband);
        }
    }

    /**
     * Computes a magnitude spectrum from a signal via FFT.
     * Returns an array of magnitudes (half the FFT size), or null if the FFT fails.
     */
    private float[] computeSpectrum(float[] signal) {
        float[] fftInput = new float[FFT_SIZE];
        int copyLen = Math.min(signal.length, FFT_SIZE);
        System.arraycopy(signal, 0, fftInput, 0, copyLen);
        // Zero-pad the remainder if signal is shorter than FFT_SIZE

        float[] spectrum = nativeDSP.fftForward(fftInput, FFT_SIZE);
        if (spectrum == null) {
            return null;
        }
        return extractMagnitudes(spectrum);
    }

    /**
     * Extracts magnitude values from interleaved complex FFT output.
     * Even indices hold magnitudes; odd indices hold phases.
     */
    private float[] extractMagnitudes(float[] spectrum) {
        float[] magnitudes = new float[spectrum.length / 2];
        for (int i = 0; i < magnitudes.length; i++) {
            magnitudes[i] = spectrum[i * 2]; // magnitude at even indices
        }
        return magnitudes;
    }

    /**
     * Computes mean power of a signal (average of squared samples).
     */
    private double computePower(float[] signal) {
        double sum = 0;
        for (float s : signal) {
            sum += (double) s * s;
        }
        return sum / signal.length;
    }

    /**
     * Estimates FSPL in dB using the analytical formula.
     * Used for SNR estimation (the native computeFSPL is used for the metadata message).
     */
    private double computeFSPLdB(float freq, double distance) {
        return 20 * Math.log10(distance) + 20 * Math.log10(freq) - 147.55;
    }

    /**
     * Releases native DSP resources.
     */
    public void shutdown() {
        nativeDSP.dspShutdown();
    }
}
