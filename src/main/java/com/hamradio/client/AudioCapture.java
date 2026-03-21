package com.hamradio.client;

/**
 * Generates TX audio from text messages.
 * Each character maps to a unique tone frequency, producing a simple
 * character-to-tone encoding suitable for transmission over the simulated channel.
 */
public class AudioCapture {

    /**
     * Generates a tone sequence from a text message.
     * Each character produces a 50ms tone at a frequency determined by its ASCII value.
     * Space characters produce silence.
     *
     * @param message    the text message to encode
     * @param sampleRate audio sample rate in Hz
     * @return float array of audio samples
     */
    public static float[] generateFromText(String message, int sampleRate) {
        int samplesPerChar = sampleRate / 20; // 50ms per character
        float[] tone = new float[message.length() * samplesPerChar];
        for (int c = 0; c < message.length(); c++) {
            float freq = 300 + (message.charAt(c) % 64) * 30;
            float amp = (message.charAt(c) == ' ') ? 0.0f : 0.8f;
            for (int i = 0; i < samplesPerChar; i++) {
                double t = (double) i / sampleRate;
                tone[c * samplesPerChar + i] = (float) (amp * Math.sin(2.0 * Math.PI * freq * t));
            }
        }
        return tone;
    }
}
