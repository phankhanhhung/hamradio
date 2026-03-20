package com.hamradio.client;

import com.hamradio.event.EventBus;
import com.hamradio.event.events.*;
import com.hamradio.protocol.Message;
import com.hamradio.protocol.messages.*;

import java.util.function.Consumer;

/**
 * Translates incoming server protocol messages into local EventBus events.
 * This bridges the network protocol layer with the UI layer, allowing
 * existing UI panels (SpectrumView, WaterfallView, etc.) to react to
 * server-pushed data via the same EventBus mechanism used in local mode.
 */
public class MessageDispatcher {

    private final EventBus eventBus;
    private Consumer<ConnectAckMessage> onConnectAck;
    private Consumer<String> onConnectNack;
    private Consumer<String> onDisconnect;

    public MessageDispatcher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    /**
     * Dispatches a received server message to the appropriate handler.
     * Protocol messages (CONNECT_ACK, CONNECT_NACK) go to registered callbacks.
     * Data messages (RX_AUDIO, SPECTRUM_DATA, etc.) are published on the EventBus.
     *
     * @param msg the message received from the server
     */
    public void dispatch(Message msg) {
        switch (msg.getType()) {
            case RX_AUDIO: {
                RxAudioMessage rxAudio = (RxAudioMessage) msg;
                // Publish as a SignalEvent so UI panels can display the received signal
                eventBus.publish(new SignalEvent(
                        this,
                        rxAudio.getSamples(),
                        0.0, // frequency is relative to the station's tuned frequency
                        0    // sample rate provided by server via ConnectAck
                ));
                break;
            }

            case SPECTRUM_DATA: {
                SpectrumDataMessage specMsg = (SpectrumDataMessage) msg;
                eventBus.publish(new SpectrumDataEvent(
                        this,
                        specMsg.getMagnitudes(),
                        specMsg.getStationId()
                ));
                break;
            }

            case STATION_UPDATE: {
                StationUpdateMessage stationMsg = (StationUpdateMessage) msg;
                eventBus.publish(new StationEvent(
                        this,
                        stationMsg.getCallsign(),
                        stationMsg.getState()
                ));
                break;
            }

            case SCENARIO_STATE: {
                ScenarioStateMessage stateMsg = (ScenarioStateMessage) msg;
                eventBus.publish(new ScenarioStateEvent(
                        this,
                        stateMsg.getOldState(),
                        stateMsg.getNewState()
                ));
                break;
            }

            case LOG: {
                LogMessage logMsg = (LogMessage) msg;
                eventBus.publish(new LogEvent(
                        this,
                        logMsg.getLevel(),
                        logMsg.getMessage()
                ));
                break;
            }

            case CONNECT_ACK: {
                ConnectAckMessage ack = (ConnectAckMessage) msg;
                if (onConnectAck != null) {
                    onConnectAck.accept(ack);
                }
                break;
            }

            case CONNECT_NACK: {
                ConnectNackMessage nack = (ConnectNackMessage) msg;
                if (onConnectNack != null) {
                    onConnectNack.accept(nack.getReason());
                }
                break;
            }

            case RX_METADATA: {
                RxMetadataMessage meta = (RxMetadataMessage) msg;
                String info = String.format("[RX %s] SNR: %.1f dB, FSPL: %.1f dB, Distance: %.0f km, Samples: %d",
                        meta.getSourceCallsign(), meta.getSnrDb(), meta.getFsplDb(),
                        meta.getDistanceMeters() / 1000.0, meta.getNumSamples());
                eventBus.publish(new LogEvent(this, "INFO", info));
                break;
            }

            case PONG: {
                PongMessage pong = (PongMessage) msg;
                long latencyMs = System.currentTimeMillis() - pong.getTimestamp();
                eventBus.publish(new LogEvent(this, "DEBUG",
                        "Server latency: " + latencyMs + " ms"));
                break;
            }

            default:
                // Unhandled message type — log for diagnostics
                eventBus.publish(new LogEvent(this, "WARN",
                        "Unhandled message type: " + msg.getType()));
                break;
        }
    }

    /**
     * Called when the connection to the server is lost unexpectedly.
     *
     * @param reason description of why the connection was lost
     */
    public void onDisconnected(String reason) {
        if (onDisconnect != null) {
            onDisconnect.accept(reason);
        }
    }

    public void setOnConnectAck(Consumer<ConnectAckMessage> handler) {
        this.onConnectAck = handler;
    }

    public void setOnConnectNack(Consumer<String> handler) {
        this.onConnectNack = handler;
    }

    public void setOnDisconnect(Consumer<String> handler) {
        this.onDisconnect = handler;
    }
}
