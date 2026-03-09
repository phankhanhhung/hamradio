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
    public static ServerConfig parse(String[] args) {
        ServerConfig config = new ServerConfig();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    if (i + 1 < args.length) {
                        config.port = parsePositiveInt(args[++i], "port");
                    } else {
                        throw new IllegalArgumentException("--port requires a value");
                    }
                    break;
                case "--sample-rate":
                    if (i + 1 < args.length) {
                        config.sampleRate = parsePositiveInt(args[++i], "sample-rate");
                    } else {
                        throw new IllegalArgumentException("--sample-rate requires a value");
                    }
                    break;
                case "--propagation":
                    if (i + 1 < args.length) {
                        String model = args[++i];
                        if (!model.equals("fspl") && !model.equals("multipath")
                                && !model.equals("ionospheric") && !model.equals("full")) {
                            throw new IllegalArgumentException(
                                    "Invalid propagation model: " + model
                                            + " (expected: fspl, multipath, ionospheric, full)");
                        }
                        config.propagationModel = model;
                    } else {
                        throw new IllegalArgumentException("--propagation requires a value");
                    }
                    break;
                case "--max-clients":
                    if (i + 1 < args.length) {
                        config.maxClients = parsePositiveInt(args[++i], "max-clients");
                    } else {
                        throw new IllegalArgumentException("--max-clients requires a value");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }
        return config;
    }

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
    public String toString() {
        return "ServerConfig{port=" + port
                + ", sampleRate=" + sampleRate
                + ", propagationModel='" + propagationModel + '\''
                + ", maxClients=" + maxClients + '}';
    }
}
