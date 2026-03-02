package com.hamradio.rf;

import com.hamradio.rf.models.FSPLModel;
import com.hamradio.rf.models.MultipathModel;
import com.hamradio.rf.models.NoiseFloorModel;

/**
 * Tests for the RF ChannelPipeline.
 * Requires the native library (hamradio) to be loaded for NativeRF operations.
 *
 * Tests:
 * - FSPL formula verification at 7 MHz / 1000 km
 * - Pipeline with FSPLModel attenuates signal
 * - Full pipeline (FSPL + multipath + noise) produces output
 */
public class ChannelPipelineTest {

    static {
        System.loadLibrary("hamradio");
    }

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testFSPLFormulaAt7MHz1000km();
        testFSPLModelAttenuatesSignal();
        testFullPipelineProducesOutput();
        testEmptyPipelinePassesThrough();
        testPipelineStageOrdering();

        System.out.println();
        System.out.println("========================================");
        System.out.println("ChannelPipelineTest Results: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- Helper methods ---

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + testName);
        } else {
            failed++;
            System.out.println("FAIL: " + testName);
        }
    }

    private static void assertApproxEqual(double expected, double actual, double tolerance, String testName) {
        double diff = Math.abs(expected - actual);
        if (diff <= tolerance) {
            passed++;
            System.out.println("PASS: " + testName + " (expected~" + expected + ", actual=" + actual + ", tol=" + tolerance + ")");
        } else {
            failed++;
            System.out.println("FAIL: " + testName + " (expected~" + expected + ", actual=" + actual + ", diff=" + diff + ", tol=" + tolerance + ")");
        }
    }

    // --- Tests ---

    /**
     * Verify FSPL formula: FSPL(dB) = 20*log10(d) + 20*log10(f) + 20*log10(4*pi/c)
     * At f=7 MHz (7e6 Hz) and d=1000 km (1e6 m):
     *   20*log10(1e6) + 20*log10(7e6) + 20*log10(4*pi/3e8)
     *   = 120 + 136.9 + (-49.54)
     *   ~ 107.4 dB
     *
     * The native implementation uses: 20*log10(4*pi*d*f/c)
     * Expected ~ 109 dB (within a few dB tolerance for implementation differences).
     */
    private static void testFSPLFormulaAt7MHz1000km() {
        System.out.println("--- testFSPLFormulaAt7MHz1000km ---");

        NativeRF nativeRF = new NativeRF();
        float frequencyHz = 7_000_000.0f;   // 7 MHz
        float distanceMeters = 1_000_000.0f; // 1000 km

        float fsplDb = nativeRF.computeFSPL(frequencyHz, distanceMeters);

        // Analytical: 20*log10(4*pi*1e6*7e6 / 3e8)
        double analytical = 20.0 * Math.log10(4.0 * Math.PI * 1_000_000.0 * 7_000_000.0 / 3e8);
        System.out.println("  Native FSPL = " + fsplDb + " dB");
        System.out.println("  Analytical  = " + analytical + " dB");

        // Expect around 109 dB, allow 3 dB tolerance
        assertApproxEqual(109.0, fsplDb, 3.0, "FSPL at 7MHz/1000km is approximately 109 dB");
        assertTrue(fsplDb > 100.0, "FSPL > 100 dB for 7MHz over 1000km");
        assertTrue(fsplDb < 120.0, "FSPL < 120 dB for 7MHz over 1000km");
    }

    /**
     * A pipeline with only FSPLModel should attenuate the signal.
     * The output amplitude should be less than the input amplitude.
     */
    private static void testFSPLModelAttenuatesSignal() {
        System.out.println("--- testFSPLModelAttenuatesSignal ---");

        ChannelPipeline pipeline = new ChannelPipeline();
        pipeline.addStage(new FSPLModel());

        // Create a simple sine wave signal
        int numSamples = 1024;
        float[] signal = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            signal[i] = (float) Math.sin(2.0 * Math.PI * 1000.0 * i / 44100.0);
        }

        // Context: 7 MHz, 1000 km
        RFContext ctx = new RFContext(7_000_000, 1_000_000, 44100);
        float[] output = pipeline.process(signal, ctx);

        assertTrue(output != null, "FSPLModel output is not null");
        assertTrue(output.length == numSamples, "FSPLModel output length matches input");

        // Compute RMS of input and output
        double rmsIn = rms(signal);
        double rmsOut = rms(output);
        System.out.println("  Input RMS  = " + rmsIn);
        System.out.println("  Output RMS = " + rmsOut);

        assertTrue(rmsOut < rmsIn, "FSPL attenuates signal (output RMS < input RMS)");
        assertTrue(rmsOut > 0.0, "Output is not zero (some signal remains)");

        // At ~109 dB loss, the linear gain is ~10^(-109/20) ~ 3.5e-6
        // So output should be very small
        assertTrue(rmsOut < rmsIn * 0.001, "Output is heavily attenuated (>60 dB loss)");
    }

    /**
     * Full pipeline: FSPL + multipath + noise floor.
     * Verifies the pipeline processes through all stages and produces non-null output.
     */
    private static void testFullPipelineProducesOutput() {
        System.out.println("--- testFullPipelineProducesOutput ---");

        ChannelPipeline pipeline = new ChannelPipeline();
        pipeline.addStage(new FSPLModel());
        pipeline.addStage(MultipathModel.createDefault());
        pipeline.addStage(NoiseFloorModel.createDefault());

        assertTrue(pipeline.getStages().size() == 3, "Pipeline has 3 stages");

        int numSamples = 2048;
        float[] signal = new float[numSamples];
        for (int i = 0; i < numSamples; i++) {
            signal[i] = (float) Math.sin(2.0 * Math.PI * 440.0 * i / 44100.0);
        }

        RFContext ctx = new RFContext(7_000_000, 500_000, 44100);
        float[] output = pipeline.process(signal, ctx);

        assertTrue(output != null, "Full pipeline output is not null");
        assertTrue(output.length > 0, "Full pipeline output has samples");

        // With noise floor added, RMS should be non-zero even after heavy attenuation
        double rmsOut = rms(output);
        System.out.println("  Full pipeline output RMS = " + rmsOut);
        assertTrue(rmsOut > 0.0, "Full pipeline output has non-zero energy");
    }

    /**
     * An empty pipeline (no stages) should pass the signal through unchanged.
     */
    private static void testEmptyPipelinePassesThrough() {
        System.out.println("--- testEmptyPipelinePassesThrough ---");

        ChannelPipeline pipeline = new ChannelPipeline();
        assertTrue(pipeline.getStages().isEmpty(), "Empty pipeline has no stages");

        float[] signal = {1.0f, 0.5f, -0.5f, -1.0f};
        RFContext ctx = new RFContext(7_000_000, 1000, 44100);
        float[] output = pipeline.process(signal, ctx);

        assertTrue(output != null, "Empty pipeline output is not null");
        assertTrue(output.length == signal.length, "Empty pipeline preserves length");

        boolean match = true;
        for (int i = 0; i < signal.length; i++) {
            if (Math.abs(signal[i] - output[i]) > 1e-6f) {
                match = false;
                break;
            }
        }
        assertTrue(match, "Empty pipeline preserves signal values exactly");
    }

    /**
     * Test that pipeline stages are applied in order by checking getStages().
     */
    private static void testPipelineStageOrdering() {
        System.out.println("--- testPipelineStageOrdering ---");

        ChannelPipeline pipeline = new ChannelPipeline();
        pipeline.addStage(new FSPLModel());
        pipeline.addStage(MultipathModel.createDefault());
        pipeline.addStage(NoiseFloorModel.createDefault());

        assertTrue(pipeline.getStages().get(0).getName().equals("fspl"), "First stage is FSPL");
        assertTrue(pipeline.getStages().get(1).getName().equals("multipath"), "Second stage is multipath");
        assertTrue(pipeline.getStages().get(2).getName().equals("noise_floor"), "Third stage is noise floor");

        // Remove multipath
        pipeline.removeStage("multipath");
        assertTrue(pipeline.getStages().size() == 2, "After removal, pipeline has 2 stages");
        assertTrue(pipeline.getStages().get(0).getName().equals("fspl"), "After removal, first stage is FSPL");
        assertTrue(pipeline.getStages().get(1).getName().equals("noise_floor"), "After removal, second stage is noise floor");
    }

    // --- Utility ---

    private static double rms(float[] data) {
        double sum = 0;
        for (float v : data) {
            sum += (double) v * v;
        }
        return Math.sqrt(sum / data.length);
    }
}
