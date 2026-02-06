package com.hamradio.dsp.graph;

public class Port {

    public enum Direction { INPUT, OUTPUT }

    private final String name;
    private final Direction direction;
    private DSPBlock owner;

    public Port(String name, Direction direction) {
        this.name = name;
        this.direction = direction;
    }

    public String getName() { return name; }
    public Direction getDirection() { return direction; }
    public DSPBlock getOwner() { return owner; }
    void setOwner(DSPBlock owner) { this.owner = owner; }

    @Override
    public String toString() {
        return (owner != null ? owner.getId() + "." : "") + name + "(" + direction + ")";
    }
}
