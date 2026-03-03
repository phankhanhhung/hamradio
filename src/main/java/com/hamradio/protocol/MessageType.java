package com.hamradio.protocol;

public enum MessageType {
    // Client → Server
    CONNECT(0x01), DISCONNECT(0x02), TX_BEGIN(0x03), TX_AUDIO(0x04),
    TX_END(0x05), TUNE(0x06), PING(0x07),
    // Server → Client
    CONNECT_ACK(0x81), CONNECT_NACK(0x82), RX_AUDIO(0x83),
    SPECTRUM_DATA(0x84), STATION_UPDATE(0x85), SCENARIO_STATE(0x86),
    LOG(0x87), PONG(0x88), RX_METADATA(0x89);

    private final int id;
    MessageType(int id) { this.id = id; }
    public int getId() { return id; }
    public static MessageType fromId(int id) {
        for (MessageType t : values()) if (t.id == id) return t;
        throw new IllegalArgumentException("Unknown message type: 0x" + Integer.toHexString(id));
    }
}
