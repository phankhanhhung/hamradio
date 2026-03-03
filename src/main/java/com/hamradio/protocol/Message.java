package com.hamradio.protocol;

import com.hamradio.protocol.messages.*;

public abstract class Message {
    public abstract MessageType getType();
    public abstract byte[] encodePayload();

    /**
     * Factory method for decoding - delegates to concrete classes based on message type.
     */
    public static Message decode(MessageType type, byte[] payload) {
        switch (type) {
            case CONNECT:
                return ConnectMessage.fromBytes(payload);
            case CONNECT_ACK:
                return ConnectAckMessage.fromBytes(payload);
            case CONNECT_NACK:
                return ConnectNackMessage.fromBytes(payload);
            case DISCONNECT:
                return DisconnectMessage.fromBytes(payload);
            case TX_BEGIN:
                return TxBeginMessage.fromBytes(payload);
            case TX_AUDIO:
                return TxAudioMessage.fromBytes(payload);
            case TX_END:
                return TxEndMessage.fromBytes(payload);
            case RX_AUDIO:
                return RxAudioMessage.fromBytes(payload);
            case RX_METADATA:
                return RxMetadataMessage.fromBytes(payload);
            case SPECTRUM_DATA:
                return SpectrumDataMessage.fromBytes(payload);
            case STATION_UPDATE:
                return StationUpdateMessage.fromBytes(payload);
            case SCENARIO_STATE:
                return ScenarioStateMessage.fromBytes(payload);
            case LOG:
                return LogMessage.fromBytes(payload);
            case TUNE:
                return TuneMessage.fromBytes(payload);
            case PING:
                return PingMessage.fromBytes(payload);
            case PONG:
                return PongMessage.fromBytes(payload);
            default:
                throw new IllegalArgumentException("Unsupported message type: " + type);
        }
    }
}
