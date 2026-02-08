package com.hamradio.dsp.graph;

import java.util.List;

public class GraphExecutor {

    private static final int DEFAULT_CHUNK_SIZE = 4096;

    private final DSPGraph graph;
    private final int chunkSize;
    private volatile boolean running;
    private Thread executionThread;

    public GraphExecutor(DSPGraph graph) {
        this(graph, DEFAULT_CHUNK_SIZE);
    }

    public GraphExecutor(DSPGraph graph, int chunkSize) {
        this.graph = graph;
        this.chunkSize = chunkSize;
    }

    public void executeCycle() {
        List<DSPBlock> order = graph.getExecutionOrder();

        for (DSPBlock block : order) {
            float[] input = null;
            int inputSize = chunkSize;

            // Read input from connected ring buffer (if block has inputs)
            if (!block.getInputs().isEmpty()) {
                Port inPort = block.getInputs().get(0);
                Connection conn = graph.getConnectionTo(inPort);
                if (conn != null) {
                    int avail = conn.getBuffer().available();
                    if (avail > 0) {
                        inputSize = Math.min(avail, chunkSize);
                        input = new float[inputSize];
                        conn.getBuffer().read(input);
                    } else {
                        continue; // No data available, skip this block
                    }
                }
            }
            // SourceBlocks have no inputs — input stays null, inputSize = chunkSize

            int outputSize = block.getOutputSize(inputSize);
            float[] output = outputSize > 0 ? new float[outputSize] : null;

            block.process(input, output, inputSize);

            // Write output to connected ring buffer
            if (output != null && !block.getOutputs().isEmpty()) {
                Port outPort = block.getOutputs().get(0);
                Connection conn = graph.getConnectionFrom(outPort);
                if (conn != null) {
                    conn.getBuffer().write(output);
                }
            }
        }
    }

    /**
     * Start continuous execution on a background thread.
     * Calls executeCycle() in a loop until stop() is called.
     */
    public void start() {
        if (running) return;
        running = true;
        graph.initialize();

        executionThread = new Thread(() -> {
            while (running) {
                try {
                    executeCycle();
                    // Yield to avoid busy-spinning; real-time pacing would use
                    // chunkSize/sampleRate to compute the sleep interval.
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[GraphExecutor] Error: " + e.getMessage());
                }
            }
        }, "dsp-graph-executor");
        executionThread.setDaemon(true);
        executionThread.start();
    }

    /**
     * Execute a single pass through the graph (non-looping).
     * Useful for batch/offline processing.
     */
    public void executeOnce() {
        graph.initialize();
        executeCycle();
    }

    public void stop() {
        running = false;
        if (executionThread != null) {
            executionThread.interrupt();
            try {
                executionThread.join(2000);
            } catch (InterruptedException ignored) { }
            executionThread = null;
        }
        graph.dispose();
    }

    public boolean isRunning() { return running; }
    public int getChunkSize() { return chunkSize; }
}
