package com.hamradio.dsp.graph;

import java.util.*;

public class DSPGraph {

    private final String name;
    private final Map<String, DSPBlock> blocks = new LinkedHashMap<>();
    private final List<Connection> connections = new ArrayList<>();
    private List<DSPBlock> executionOrder;
    private List<DSPBlock> executionOrderView;

    public DSPGraph(String name) {
        this.name = name;
    }

    public void addBlock(DSPBlock block) {
        blocks.put(block.getId(), block);
        executionOrder = null;
        executionOrderView = null;
    }

    public void connect(Port source, Port destination) {
        if (source.getDirection() != Port.Direction.OUTPUT) {
            throw new IllegalArgumentException("Source port must be OUTPUT: " + source);
        }
        if (destination.getDirection() != Port.Direction.INPUT) {
            throw new IllegalArgumentException("Destination port must be INPUT: " + destination);
        }
        Connection conn = new Connection(source, destination);
        connections.add(conn);
        executionOrder = null;
        executionOrderView = null;
    }

    public void connect(String sourceBlockId, String sourcePort, String destBlockId, String destPort) {
        DSPBlock src = blocks.get(sourceBlockId);
        DSPBlock dst = blocks.get(destBlockId);
        if (src == null) throw new IllegalArgumentException("Block not found: " + sourceBlockId);
        if (dst == null) throw new IllegalArgumentException("Block not found: " + destBlockId);
        Port sp = src.getOutput(sourcePort);
        Port dp = dst.getInput(destPort);
        if (sp == null) throw new IllegalArgumentException("Output port not found: " + sourceBlockId + "." + sourcePort);
        if (dp == null) throw new IllegalArgumentException("Input port not found: " + destBlockId + "." + destPort);
        connect(sp, dp);
    }

    public void validate() {
        topologicalSort();
    }

    public List<DSPBlock> getExecutionOrder() {
        if (executionOrder == null) {
            executionOrder = topologicalSort();
            executionOrderView = Collections.unmodifiableList(executionOrder);
        }
        return executionOrderView;
    }

    private List<DSPBlock> topologicalSort() {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (String id : blocks.keySet()) {
            inDegree.put(id, 0);
            adjacency.put(id, new ArrayList<>());
        }

        for (Connection conn : connections) {
            String srcId = conn.getSource().getOwner().getId();
            String dstId = conn.getDestination().getOwner().getId();
            adjacency.get(srcId).add(dstId);
            inDegree.merge(dstId, 1, Integer::sum);
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) queue.add(e.getKey());
        }

        List<DSPBlock> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            sorted.add(blocks.get(id));
            for (String neighbor : adjacency.get(id)) {
                int deg = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, deg);
                if (deg == 0) queue.add(neighbor);
            }
        }

        if (sorted.size() != blocks.size()) {
            throw new IllegalStateException("Graph contains a cycle");
        }

        return sorted;
    }

    public Connection getConnectionTo(Port inputPort) {
        for (Connection c : connections) {
            if (c.getDestination() == inputPort) return c;
        }
        return null;
    }

    public Connection getConnectionFrom(Port outputPort) {
        for (Connection c : connections) {
            if (c.getSource() == outputPort) return c;
        }
        return null;
    }

    public void initialize() {
        for (DSPBlock block : getExecutionOrder()) {
            block.initialize();
        }
    }

    public void dispose() {
        for (DSPBlock block : blocks.values()) {
            block.dispose();
        }
        for (Connection conn : connections) {
            conn.close();
        }
    }

    public String getName() { return name; }
    public DSPBlock getBlock(String id) { return blocks.get(id); }
    public Collection<DSPBlock> getBlocks() { return blocks.values(); }
    public List<Connection> getConnections() { return Collections.unmodifiableList(connections); }
}
