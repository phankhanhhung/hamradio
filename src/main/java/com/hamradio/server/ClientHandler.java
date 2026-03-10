package com.hamradio.server;

import com.hamradio.net.Station;
import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.MessageType;
import com.hamradio.protocol.messages.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles a single client connection on its own reader thread.
 * <p>
 * Lifecycle:
 *  1. The first message must be CONNECT -- the handler creates a Station, registers itself,
 *     replies with CONNECT_ACK, and notifies all other clients with STATION_UPDATE.
 *  2. Subsequent messages are dispatched by type (TX_BEGIN, TX_AUDIO, TX_END, TUNE, PING, DISCONNECT).
 *  3. On disconnect or error the handler unregisters, notifies peers, and closes the socket.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final SimulationEngine engine;
    private final ClientRegistry registry;
    private final ServerConfig config;
    private OutputStream out;
    private InputStream in;
    private String callsign;
    private Station station;
    private volatile boolean running;

    public ClientHandler(Socket socket, SimulationEngine engine,
                         ClientRegistry registry, ServerConfig config) {
        this.socket = socket;
        this.engine = engine;
        this.registry = registry;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            running = true;

            // ---------- Step 1: first message must be CONNECT ----------
            Message firstMsg = MessageCodec.read(in);
            if (firstMsg.getType() != MessageType.CONNECT) {
                sendMessage(new ConnectNackMessage("First message must be CONNECT"));
                close();
                return;
            }

            ConnectMessage connectMsg = (ConnectMessage) firstMsg;
            callsign = connectMsg.getCallsign();

            // Reject duplicate callsigns
            if (registry.getHandler(callsign) != null) {
                sendMessage(new ConnectNackMessage("Callsign already connected: " + callsign));
                close();
                return;
            }

            // ---------- Step 2: create Station and register ----------
            station = new Station(
                    callsign,
                    connectMsg.getLatitude(),
                    connectMsg.getLongitude(),
                    connectMsg.getFrequencyHz(),
                    connectMsg.getMode());
            station.setUpperSideband(connectMsg.isUpperSideband());
            station.setTxRxState(Station.TxRxState.IDLE);

            registry.register(callsign, this);

            System.out.println("[Server] Client connected: " + callsign
                    + " (" + connectMsg.getLatitude() + ", " + connectMsg.getLongitude() + ")"
                    + " freq=" + connectMsg.getFrequencyHz()
                    + " mode=" + connectMsg.getMode());

            // ---------- Step 3: send CONNECT_ACK ----------
            String sessionId = UUID.randomUUID().toString();
            List<String> otherCallsigns = new ArrayList<>();
            for (ClientHandler h : registry.getAllHandlers()) {
                if (!callsign.equals(h.getCallsign())) {
                    otherCallsigns.add(h.getCallsign());
                }
            }
            ConnectAckMessage ack = new ConnectAckMessage(
                    sessionId,
                    config.getSampleRate(),
                    config.getPropagationModel(),
                    otherCallsigns);
            sendMessage(ack);

            // ---------- Step 4: notify other clients ----------
            StationUpdateMessage joinUpdate = new StationUpdateMessage(
                    callsign,
                    Station.TxRxState.IDLE.name(),
                    station.getFrequencyHz());
            registry.broadcastExcept(callsign, joinUpdate);

            // ---------- Step 5: message read loop ----------
            while (running) {
                Message msg;
                try {
                    msg = MessageCodec.read(in);
                } catch (EOFException | SocketException e) {
                    // Client disconnected normally
                    break;
                }

                switch (msg.getType()) {
                    case TX_BEGIN:
                        handleTxBegin((TxBeginMessage) msg);
                        break;
                    case TX_AUDIO:
                        handleTxAudio((TxAudioMessage) msg);
                        break;
                    case TX_END:
                        handleTxEnd((TxEndMessage) msg);
                        break;
                    case TUNE:
                        handleTune((TuneMessage) msg);
                        break;
                    case PING:
                        handlePing((PingMessage) msg);
                        break;
                    case DISCONNECT:
                        running = false;
                        break;
                    default:
                        System.err.println("[Server] Unexpected message type from "
                                + callsign + ": " + msg.getType());
                        break;
                }
            }
        } catch (EOFException | SocketException e) {
            // Normal disconnection path
        } catch (IOException e) {
            System.err.println("[Server] IO error for "
                    + (callsign != null ? callsign : "unknown") + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[Server] Unexpected error for "
                    + (callsign != null ? callsign : "unknown") + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ---------- Message handlers ----------

    private void handleTxBegin(TxBeginMessage msg) {
        station.setTxRxState(Station.TxRxState.TRANSMITTING);
        System.out.println("[Server] TX_BEGIN from " + callsign);
        StationUpdateMessage update = new StationUpdateMessage(
                callsign,
                Station.TxRxState.TRANSMITTING.name(),
                station.getFrequencyHz());
        registry.broadcastExcept(callsign, update);
    }

    private void handleTxAudio(TxAudioMessage msg) {
        try {
            engine.processTxAudio(callsign, msg.getSamples());
        } catch (Exception e) {
            System.err.println("[Server] processTxAudio error from "
                    + callsign + ": " + e.getMessage());
        }
    }

    private void handleTxEnd(TxEndMessage msg) {
        station.setTxRxState(Station.TxRxState.IDLE);
        System.out.println("[Server] TX_END from " + callsign
                + " (" + msg.getTotalSamples() + " total samples)");
        StationUpdateMessage update = new StationUpdateMessage(
                callsign,
                Station.TxRxState.IDLE.name(),
                station.getFrequencyHz());
        registry.broadcastExcept(callsign, update);
    }

    private void handleTune(TuneMessage msg) {
        station.setFrequencyHz(msg.getFrequencyHz());
        station.setMode(msg.getMode());
        station.setUpperSideband(msg.isUpperSideband());
        System.out.println("[Server] TUNE from " + callsign
                + ": freq=" + msg.getFrequencyHz() + " mode=" + msg.getMode());
        StationUpdateMessage update = new StationUpdateMessage(
                callsign,
                station.getTxRxState().name(),
                station.getFrequencyHz());
        registry.broadcast(update);
    }

    private void handlePing(PingMessage msg) {
        try {
            sendMessage(new PongMessage(msg.getTimestamp()));
        } catch (Exception e) {
            System.err.println("[Server] Failed to send PONG to " + callsign);
        }
    }

    // ---------- Outbound messaging ----------

    /**
     * Writes a framed message to this client's output stream.
     * Synchronized so that concurrent callers (e.g. SimulationEngine threads
     * and broadcast) do not interleave frame bytes.
     */
    public synchronized void sendMessage(Message msg) {
        try {
            if (out != null && !socket.isClosed()) {
                MessageCodec.write(out, msg);
            }
        } catch (IOException e) {
            System.err.println("[Server] sendMessage failed to "
                    + (callsign != null ? callsign : "unknown") + ": " + e.getMessage());
            running = false;
        }
    }

    // ---------- Cleanup ----------

    private void cleanup() {
        running = false;
        if (callsign != null) {
            registry.unregister(callsign);
            // Notify remaining clients that this station left
            StationUpdateMessage leaveUpdate = new StationUpdateMessage(
                    callsign, "DISCONNECTED", 0);
            registry.broadcast(leaveUpdate);
            System.out.println("[Server] Client disconnected: " + callsign);
        }
        close();
    }

    /**
     * Closes the underlying socket (and its streams).
     */
    public void close() {
        running = false;
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore close errors
        }
    }

    // ---------- Accessors ----------

    public String getCallsign() {
        return callsign;
    }

    public Station getStation() {
        return station;
    }

    public boolean isRunning() {
        return running;
    }
}
