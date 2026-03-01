package com.hamradio.dsp.graph;

import java.util.List;

/**
 * Tests for DSPGraph: topological sort ordering, cycle detection, and basic structure.
 *
 * NOTE: We do NOT test ring buffer / NativeBuffer paths here since those require
 * the native library. We use lightweight stub blocks to test graph structure only.
 */
public class DSPGraphTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testLinearChainTopologicalOrder();
        testDiamondGraphTopologicalOrder();
        testCycleDetectionThrows();
        testSingleBlockNoConnections();
        testDisconnectedBlocks();
        testInvalidPortDirectionThrows();
        testConnectByIdWithMissingBlockThrows();
        testGetExecutionOrderCachesResult();

        System.out.println();
        System.out.println("========================================");
        System.out.println("DSPGraphTest Results: " + passed + " passed, " + failed + " failed");
        System.out.println("========================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- Stub blocks (no native dependencies) ---

    /** A minimal source block with one output port. */
    private static class StubSource extends DSPBlock {
        StubSource(String id) {
            super(id);
            addOutput("out");
        }
        @Override
        public void process(float[] input, float[] output, int numSamples) {
            // No-op for graph structure tests
        }
        @Override
        public int getOutputSize(int inputSize) { return inputSize; }
    }

    /** A minimal processing block with one input and one output port. */
    private static class StubProcessor extends DSPBlock {
        StubProcessor(String id) {
            super(id);
            addInput("in");
            addOutput("out");
        }
        @Override
        public void process(float[] input, float[] output, int numSamples) {
            // No-op for graph structure tests
        }
        @Override
        public int getOutputSize(int inputSize) { return inputSize; }
    }

    /** A minimal sink block with one input port. */
    private static class StubSink extends DSPBlock {
        StubSink(String id) {
            super(id);
            addInput("in");
        }
        @Override
        public void process(float[] input, float[] output, int numSamples) {
            // No-op for graph structure tests
        }
        @Override
        public int getOutputSize(int inputSize) { return 0; }
    }

    // --- Helper methods ---

    private static void assertEqual(Object expected, Object actual, String testName) {
        if (expected == null && actual == null) {
            passed++;
            System.out.println("PASS: " + testName);
        } else if (expected != null && expected.equals(actual)) {
            passed++;
            System.out.println("PASS: " + testName);
        } else {
            failed++;
            System.out.println("FAIL: " + testName + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + testName);
        } else {
            failed++;
            System.out.println("FAIL: " + testName);
        }
    }

    // --- Tests ---

    /**
     * Linear chain: Source -> Processor -> Sink
     * Topological order must be [Source, Processor, Sink].
     */
    private static void testLinearChainTopologicalOrder() {
        System.out.println("--- testLinearChainTopologicalOrder ---");
        DSPGraph graph = new DSPGraph("linear");

        StubSource src = new StubSource("source");
        StubProcessor proc = new StubProcessor("processor");
        StubSink sink = new StubSink("sink");

        graph.addBlock(src);
        graph.addBlock(proc);
        graph.addBlock(sink);

        graph.connect(src.getOutput("out"), proc.getInput("in"));
        graph.connect(proc.getOutput("out"), sink.getInput("in"));

        List<DSPBlock> order = graph.getExecutionOrder();
        assertEqual(3, order.size(), "Linear chain has 3 blocks in order");
        assertEqual("source", order.get(0).getId(), "First block is source");
        assertEqual("processor", order.get(1).getId(), "Second block is processor");
        assertEqual("sink", order.get(2).getId(), "Third block is sink");
    }

    /**
     * Diamond graph:
     *        Source
     *       /      \
     *    ProcA    ProcB
     *       \      /
     *        Sink (with two inputs)
     *
     * Source must come before ProcA and ProcB. Both must come before Sink.
     */
    private static void testDiamondGraphTopologicalOrder() {
        System.out.println("--- testDiamondGraphTopologicalOrder ---");
        DSPGraph graph = new DSPGraph("diamond");

        StubSource src = new StubSource("source");
        // ProcA and ProcB each have their own in/out
        StubProcessor procA = new StubProcessor("procA");
        StubProcessor procB = new StubProcessor("procB");

        // Sink with two input ports
        DSPBlock sink = new DSPBlock("sink") {
            {
                addInput("inA");
                addInput("inB");
            }
            @Override
            public void process(float[] input, float[] output, int numSamples) {}
            @Override
            public int getOutputSize(int inputSize) { return 0; }
        };

        // Source has only one output, but we need to fan out.
        // Add a second output to source for the diamond.
        DSPBlock fanSource = new DSPBlock("source2") {
            {
                addOutput("out");
            }
            @Override
            public void process(float[] input, float[] output, int numSamples) {}
            @Override
            public int getOutputSize(int inputSize) { return inputSize; }
        };

        // Simpler approach: use source -> procA, fanSource -> procB, procA -> sink.inA, procB -> sink.inB
        graph.addBlock(src);
        graph.addBlock(fanSource);
        graph.addBlock(procA);
        graph.addBlock(procB);
        graph.addBlock(sink);

        graph.connect(src.getOutput("out"), procA.getInput("in"));
        graph.connect(fanSource.getOutput("out"), procB.getInput("in"));
        graph.connect(procA.getOutput("out"), sink.getInput("inA"));
        graph.connect(procB.getOutput("out"), sink.getInput("inB"));

        List<DSPBlock> order = graph.getExecutionOrder();
        assertEqual(5, order.size(), "Diamond graph has 5 blocks");

        // Find positions
        int srcIdx = -1, fanIdx = -1, procAIdx = -1, procBIdx = -1, sinkIdx = -1;
        for (int i = 0; i < order.size(); i++) {
            String id = order.get(i).getId();
            if (id.equals("source")) srcIdx = i;
            else if (id.equals("source2")) fanIdx = i;
            else if (id.equals("procA")) procAIdx = i;
            else if (id.equals("procB")) procBIdx = i;
            else if (id.equals("sink")) sinkIdx = i;
        }

        assertTrue(srcIdx < procAIdx, "source comes before procA");
        assertTrue(fanIdx < procBIdx, "source2 comes before procB");
        assertTrue(procAIdx < sinkIdx, "procA comes before sink");
        assertTrue(procBIdx < sinkIdx, "procB comes before sink");
    }

    /**
     * Creating a cycle (A -> B -> A) must throw IllegalStateException
     * when we request the execution order.
     */
    private static void testCycleDetectionThrows() {
        System.out.println("--- testCycleDetectionThrows ---");
        DSPGraph graph = new DSPGraph("cyclic");

        StubProcessor blockA = new StubProcessor("A");
        StubProcessor blockB = new StubProcessor("B");

        graph.addBlock(blockA);
        graph.addBlock(blockB);

        // A.out -> B.in
        graph.connect(blockA.getOutput("out"), blockB.getInput("in"));
        // B.out -> A.in (creates cycle)
        graph.connect(blockB.getOutput("out"), blockA.getInput("in"));

        boolean threw = false;
        try {
            graph.getExecutionOrder();
        } catch (IllegalStateException e) {
            threw = true;
            assertTrue(e.getMessage().contains("cycle"), "Exception message mentions cycle");
        }
        assertTrue(threw, "Cycle detection throws IllegalStateException");
    }

    /**
     * A single block with no connections should produce an execution order of size 1.
     */
    private static void testSingleBlockNoConnections() {
        System.out.println("--- testSingleBlockNoConnections ---");
        DSPGraph graph = new DSPGraph("single");

        StubSource src = new StubSource("lone");
        graph.addBlock(src);

        List<DSPBlock> order = graph.getExecutionOrder();
        assertEqual(1, order.size(), "Single block graph has 1 in execution order");
        assertEqual("lone", order.get(0).getId(), "Single block is the lone source");
    }

    /**
     * Multiple disconnected blocks should all appear in the execution order.
     */
    private static void testDisconnectedBlocks() {
        System.out.println("--- testDisconnectedBlocks ---");
        DSPGraph graph = new DSPGraph("disconnected");

        StubSource a = new StubSource("a");
        StubSource b = new StubSource("b");
        StubSource c = new StubSource("c");

        graph.addBlock(a);
        graph.addBlock(b);
        graph.addBlock(c);

        List<DSPBlock> order = graph.getExecutionOrder();
        assertEqual(3, order.size(), "3 disconnected blocks all in execution order");
    }

    /**
     * Connecting an INPUT port as the source should throw IllegalArgumentException.
     */
    private static void testInvalidPortDirectionThrows() {
        System.out.println("--- testInvalidPortDirectionThrows ---");
        DSPGraph graph = new DSPGraph("badport");

        StubProcessor blockA = new StubProcessor("A");
        StubProcessor blockB = new StubProcessor("B");

        graph.addBlock(blockA);
        graph.addBlock(blockB);

        boolean threw = false;
        try {
            // Try to use an INPUT port as source -- should fail
            graph.connect(blockA.getInput("in"), blockB.getInput("in"));
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw, "Connecting INPUT as source throws IllegalArgumentException");
    }

    /**
     * Connecting by ID with a non-existent block should throw IllegalArgumentException.
     */
    private static void testConnectByIdWithMissingBlockThrows() {
        System.out.println("--- testConnectByIdWithMissingBlockThrows ---");
        DSPGraph graph = new DSPGraph("missing");

        StubSource src = new StubSource("source");
        graph.addBlock(src);

        boolean threw = false;
        try {
            graph.connect("source", "out", "nonexistent", "in");
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        assertTrue(threw, "Connecting to non-existent block throws IllegalArgumentException");
    }

    /**
     * getExecutionOrder() should cache its result -- calling it twice returns the same list.
     */
    private static void testGetExecutionOrderCachesResult() {
        System.out.println("--- testGetExecutionOrderCachesResult ---");
        DSPGraph graph = new DSPGraph("cached");

        StubSource src = new StubSource("s");
        StubSink sink = new StubSink("t");
        graph.addBlock(src);
        graph.addBlock(sink);
        graph.connect(src.getOutput("out"), sink.getInput("in"));

        List<DSPBlock> order1 = graph.getExecutionOrder();
        List<DSPBlock> order2 = graph.getExecutionOrder();

        // They should be the same object reference (cached)
        assertTrue(order1 == order2, "getExecutionOrder() returns cached list on second call");
    }
}
