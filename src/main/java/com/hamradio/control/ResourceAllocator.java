package com.hamradio.control;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResourceAllocator {

    private ExecutorService dspThreadPool;
    private final int numThreads;

    public ResourceAllocator() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ResourceAllocator(int numThreads) {
        this.numThreads = numThreads;
    }

    public void initialize() {
        dspThreadPool = Executors.newFixedThreadPool(numThreads);
    }

    public ExecutorService getDspThreadPool() {
        return dspThreadPool;
    }

    public void shutdown() {
        if (dspThreadPool != null) {
            dspThreadPool.shutdownNow();
            dspThreadPool = null;
        }
    }

    public int getNumThreads() { return numThreads; }
}
