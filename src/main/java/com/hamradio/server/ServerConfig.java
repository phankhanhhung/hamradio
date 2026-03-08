package com.hamradio.server;

/**
 * Simple POJO holding server configuration with a command-line argument parser.
 */
public class ServerConfig {

    private int port = 7100;
    private int sampleRate = 44100;
    private String propagationModel = "full";
    private int maxClients = 10;

    public int getPort() {
        return port;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public String getPropagationModel() {
        return propagationModel;
    }

    public int getMaxClients() {
        return maxClients;
    }

    /**
     * Parses command-line arguments into a ServerConfig.
     * Supported flags:
     *   --port <int>          TCP listen port (default 7100)
     *   --sample-rate <int>   Audio sample rate in Hz (default 44100)
     *   --propagation <str>   Propagation model: fspl | multipath | ionospheric | full (default full)
     *   --max-clients <int>   Maximum simultaneous connections (default 10)
     */

    private static int parsePositiveInt(String value, String name) {
        try {
            int n = Integer.parseInt(value);
            if (n <= 0) {
                throw new IllegalArgumentException("--" + name + " must be positive, got: " + n);
            }
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--" + name + " requires an integer, got: " + value);
        }
    }

    @Override
}
