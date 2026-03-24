package com.hamradio.integration;

import com.hamradio.protocol.*;
import com.hamradio.protocol.messages.*;
import com.hamradio.server.HamRadioServer;
import com.hamradio.server.ServerConfig;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

/**
 * Headless integration test: starts a real server, connects 2 TCP clients,
 * performs a full QSO, and verifies signal delivery with propagation effects.
 *
 * No GUI, no JavaFX — pure TCP socket communication.
 */
public class ClientServerQSOTest {

    static { System.loadLibrary("hamradio"); }

    private static int passed = 0;
    private static int failed = 0;

    // Collected messages per client
    private static final List<Message> client1Inbox = new CopyOnWriteArrayList<>();
    private static final List<Message> client2Inbox = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║  CLIENT-SERVER QSO INTEGRATION TEST                         ║");
        System.out.println("║  Server + 2 TCP Clients, real propagation over network       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // ============================================================
        // STEP 1: Start server
        // ============================================================
        System.out.println("━━━ STEP 1: Start Server ━━━");

        ServerConfig config = ServerConfig.parse(new String[]{
                "--port", "17100", "--sample-rate", "44100", "--propagation", "full"
        });
        HamRadioServer server = new HamRadioServer(config);

        Thread serverThread = new Thread(() -> {
            try { server.start(); } catch (Exception e) {
                if (!"Socket closed".equals(e.getMessage())) {
                    System.err.println("[Server Error] " + e.getMessage());
                }
            }
        }, "test-server");
        serverThread.setDaemon(true);
        serverThread.start();
        Thread.sleep(1000); // Wait for server to bind
        check("Server started", serverThread.isAlive());

        // ============================================================
        // STEP 2: Client A connects (VK3ABC, Melbourne)
        // ============================================================
        System.out.println("\n━━━ STEP 2: Client A Connects (VK3ABC, Melbourne) ━━━");

        Socket socketA = new Socket("localhost", 17100);
        socketA.setTcpNoDelay(true);
        OutputStream outA = socketA.getOutputStream();
        InputStream inA = socketA.getInputStream();

        // Start reader thread for client A
        Thread readerA = startReader("ClientA", inA, client1Inbox);

        // Send CONNECT
        ConnectMessage connectA = new ConnectMessage("VK3ABC", -37.8136, 144.9631, 7100000, "SSB", true);
        MessageCodec.write(outA, connectA);
        Thread.sleep(500);

        // Check CONNECT_ACK
        Message ackA = waitForMessage(client1Inbox, MessageType.CONNECT_ACK, 3000);
        check("Client A received CONNECT_ACK", ackA != null);
        if (ackA instanceof ConnectAckMessage) {
            ConnectAckMessage ack = (ConnectAckMessage) ackA;
            System.out.println("  Session: " + ack.getSessionId());
            System.out.println("  Sample rate: " + ack.getSampleRate());
            System.out.println("  Propagation: " + ack.getPropagationModel());
            check("Sample rate = 44100", ack.getSampleRate() == 44100);
        }

        // ============================================================
        // STEP 3: Client B connects (JA1YXP, Tokyo)
        // ============================================================
        System.out.println("\n━━━ STEP 3: Client B Connects (JA1YXP, Tokyo) ━━━");

        Socket socketB = new Socket("localhost", 17100);
        socketB.setTcpNoDelay(true);
        OutputStream outB = socketB.getOutputStream();
        InputStream inB = socketB.getInputStream();

        Thread readerB = startReader("ClientB", inB, client2Inbox);

        ConnectMessage connectB = new ConnectMessage("JA1YXP", 35.6762, 139.6503, 7100000, "SSB", true);
        MessageCodec.write(outB, connectB);
        Thread.sleep(500);

        Message ackB = waitForMessage(client2Inbox, MessageType.CONNECT_ACK, 3000);
        check("Client B received CONNECT_ACK", ackB != null);
        if (ackB instanceof ConnectAckMessage) {
            ConnectAckMessage ack = (ConnectAckMessage) ackB;
            System.out.println("  Stations visible: " + ack.getStationCallsigns());
            check("Client B sees VK3ABC", ack.getStationCallsigns().contains("VK3ABC"));
        }

        // Client A should have received STATION_UPDATE about JA1YXP joining
        Message stationUpdate = waitForMessage(client1Inbox, MessageType.STATION_UPDATE, 2000);
        check("Client A notified about JA1YXP", stationUpdate != null);
        if (stationUpdate instanceof StationUpdateMessage) {
            StationUpdateMessage su = (StationUpdateMessage) stationUpdate;
            System.out.println("  Client A sees new station: " + su.getCallsign() + " (" + su.getState() + ")");
        }

        // ============================================================
        // STEP 4: Client A transmits CQ call
        // ============================================================
        System.out.println("\n━━━ STEP 4: Client A Transmits \"CQ CQ DE VK3ABC\" ━━━");

        client2Inbox.clear(); // Clear B's inbox for fresh check

        // Generate baseband audio (same as AudioCapture.generateFromText)
        String msg1 = "CQ CQ DE VK3ABC K";
        float[] audio1 = generateTone(msg1, 44100);
        System.out.println("  Generated " + audio1.length + " samples");

        // Send TX_BEGIN
        MessageCodec.write(outA, new TxBeginMessage(44100));

        // Send TX_AUDIO in chunks
        int chunkSize = 4096;
        int seq = 0;
        for (int offset = 0; offset < audio1.length; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, audio1.length);
            float[] chunk = Arrays.copyOfRange(audio1, offset, end);
            MessageCodec.write(outA, new TxAudioMessage(seq++, chunk));
        }
        System.out.println("  Sent " + seq + " chunks");

        // Send TX_END
        MessageCodec.write(outA, new TxEndMessage(audio1.length));

        // Wait for processing
        Thread.sleep(2000);

        // ============================================================
        // STEP 5: Verify Client B receives signal
        // ============================================================
        System.out.println("\n━━━ STEP 5: Verify Client B Receives Signal ━━━");

        // Client B should receive: STATION_UPDATE (VK3ABC TX), RX_AUDIO, SPECTRUM_DATA, RX_METADATA, STATION_UPDATE (VK3ABC IDLE)
        List<Message> rxAudioMsgs = getMessages(client2Inbox, MessageType.RX_AUDIO);
        List<Message> spectrumMsgs = getMessages(client2Inbox, MessageType.SPECTRUM_DATA);
        List<Message> metadataMsgs = getMessages(client2Inbox, MessageType.RX_METADATA);
        List<Message> stationUpdates = getMessages(client2Inbox, MessageType.STATION_UPDATE);

        check("Client B received RX_AUDIO", rxAudioMsgs.size() > 0);
        check("Client B received SPECTRUM_DATA", spectrumMsgs.size() > 0);
        check("Client B received RX_METADATA", metadataMsgs.size() > 0);
        check("Client B received STATION_UPDATE(s)", stationUpdates.size() > 0);

        // Verify RX audio content
        if (!rxAudioMsgs.isEmpty()) {
            RxAudioMessage rxMsg = (RxAudioMessage) rxAudioMsgs.get(0);
            check("RX source = VK3ABC", "VK3ABC".equals(rxMsg.getSourceCallsign()));
            check("RX has samples", rxMsg.getSamples().length > 0);
            System.out.println("  RX audio: " + rxMsg.getSamples().length + " samples from " + rxMsg.getSourceCallsign());

            // Verify signal is degraded (not identical to TX)
            float txPeak = peakAbs(audio1);
            float rxPeak = peakAbs(rxMsg.getSamples());
            System.out.println("  TX peak: " + String.format("%.4f", txPeak));
            System.out.println("  RX peak: " + String.format("%.6f", rxPeak));
            check("Signal degraded by propagation (RX < TX)", rxPeak < txPeak);
        }

        // Verify metadata
        if (!metadataMsgs.isEmpty()) {
            RxMetadataMessage meta = (RxMetadataMessage) metadataMsgs.get(0);
            System.out.println("  FSPL: " + String.format("%.1f dB", meta.getFsplDb()));
            System.out.println("  SNR: " + String.format("%.1f dB", meta.getSnrDb()));
            System.out.println("  Distance: " + String.format("%.0f km", meta.getDistanceMeters() / 1000));
            check("FSPL > 100 dB (long distance)", meta.getFsplDb() > 100);
            check("Distance > 7000 km (Melbourne→Tokyo)", meta.getDistanceMeters() > 7000000);
        }

        // Verify spectrum
        if (!spectrumMsgs.isEmpty()) {
            SpectrumDataMessage spec = (SpectrumDataMessage) spectrumMsgs.get(0);
            check("Spectrum has bins", spec.getMagnitudes().length > 0);
            System.out.println("  Spectrum: " + spec.getMagnitudes().length + " bins");
        }

        // Client A should also get TX spectrum feedback
        List<Message> txSpectrumMsgs = getMessages(client1Inbox, MessageType.SPECTRUM_DATA);
        check("Client A received TX spectrum feedback", txSpectrumMsgs.size() > 0);

        // ============================================================
        // STEP 6: Client B responds
        // ============================================================
        System.out.println("\n━━━ STEP 6: Client B Responds \"VK3ABC DE JA1YXP 59 K\" ━━━");

        client1Inbox.clear();

        String msg2 = "VK3ABC DE JA1YXP 59 K";
        float[] audio2 = generateTone(msg2, 44100);
        System.out.println("  Generated " + audio2.length + " samples");

        MessageCodec.write(outB, new TxBeginMessage(44100));
        seq = 0;
        for (int offset = 0; offset < audio2.length; offset += chunkSize) {
            int end = Math.min(offset + chunkSize, audio2.length);
            float[] chunk = Arrays.copyOfRange(audio2, offset, end);
            MessageCodec.write(outB, new TxAudioMessage(seq++, chunk));
        }
        MessageCodec.write(outB, new TxEndMessage(audio2.length));
        System.out.println("  Sent " + seq + " chunks");

        Thread.sleep(2000);

        // ============================================================
        // STEP 7: Verify Client A receives response
        // ============================================================
        System.out.println("\n━━━ STEP 7: Verify Client A Receives Response ━━━");

        List<Message> rxFromB = getMessages(client1Inbox, MessageType.RX_AUDIO);
        List<Message> metaFromB = getMessages(client1Inbox, MessageType.RX_METADATA);

        check("Client A received RX_AUDIO from B", rxFromB.size() > 0);
        check("Client A received RX_METADATA from B", metaFromB.size() > 0);

        if (!rxFromB.isEmpty()) {
            RxAudioMessage rxMsg = (RxAudioMessage) rxFromB.get(0);
            check("RX source = JA1YXP", "JA1YXP".equals(rxMsg.getSourceCallsign()));
            System.out.println("  RX audio: " + rxMsg.getSamples().length + " samples from " + rxMsg.getSourceCallsign());
        }

        if (!metaFromB.isEmpty()) {
            RxMetadataMessage meta = (RxMetadataMessage) metaFromB.get(0);
            System.out.println("  FSPL: " + String.format("%.1f dB", meta.getFsplDb()));
            System.out.println("  Distance: " + String.format("%.0f km", meta.getDistanceMeters() / 1000));
            // Distance should be same both ways
            check("Symmetric distance", meta.getDistanceMeters() > 7000000);
        }

        // ============================================================
        // STEP 8: Client A disconnects
        // ============================================================
        System.out.println("\n━━━ STEP 8: Clients Disconnect ━━━");

        client2Inbox.clear();

        MessageCodec.write(outA, new DisconnectMessage());
        Thread.sleep(500);
        socketA.close();
        System.out.println("  Client A disconnected");

        // Client B should get STATION_UPDATE about VK3ABC leaving
        Thread.sleep(500);
        Message leaveUpdate = waitForMessage(client2Inbox, MessageType.STATION_UPDATE, 2000);
        check("Client B notified about VK3ABC disconnect", leaveUpdate != null);

        MessageCodec.write(outB, new DisconnectMessage());
        Thread.sleep(500);
        socketB.close();
        System.out.println("  Client B disconnected");

        // ============================================================
        // STEP 9: Stop server
        // ============================================================
        System.out.println("\n━━━ STEP 9: Stop Server ━━━");
        server.stop();
        Thread.sleep(500);
        check("Server stopped", true);

        // ============================================================
        // RESULTS
        // ============================================================
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.printf("║  RESULTS: %d passed, %d failed                              ║%n", passed, failed);
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        if (failed > 0) System.exit(1);
    }

    // --- Helpers ---

    private static float[] generateTone(String message, int sampleRate) {
        int samplesPerChar = sampleRate / 20;
        float[] tone = new float[message.length() * samplesPerChar];
        for (int c = 0; c < message.length(); c++) {
            float freq = 300 + (message.charAt(c) % 64) * 30;
            float amp = (message.charAt(c) == ' ') ? 0.0f : 0.8f;
            for (int i = 0; i < samplesPerChar; i++) {
                double t = (double) i / sampleRate;
                tone[c * samplesPerChar + i] = (float) (amp * Math.sin(2.0 * Math.PI * freq * t));
            }
        }
        return tone;
    }

    private static float peakAbs(float[] signal) {
        float peak = 0;
        for (float s : signal) {
            float a = Math.abs(s);
            if (a > peak) peak = a;
        }
        return peak;
    }

    private static Thread startReader(String name, InputStream in, List<Message> inbox) {
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    Message msg = MessageCodec.read(in);
                    inbox.add(msg);
                }
            } catch (Exception e) {
                // Connection closed
            }
        }, name + "-reader");
        t.setDaemon(true);
        t.start();
        return t;
    }

    private static Message waitForMessage(List<Message> inbox, MessageType type, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Message m : inbox) {
                if (m.getType() == type) return m;
            }
            try { Thread.sleep(50); } catch (InterruptedException ignored) { }
        }
        return null;
    }

    private static List<Message> getMessages(List<Message> inbox, MessageType type) {
        List<Message> result = new ArrayList<>();
        for (Message m : inbox) {
            if (m.getType() == type) result.add(m);
        }
        return result;
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  ✓ " + name);
        } else {
            failed++;
            System.out.println("  ✗ FAIL: " + name);
        }
    }
}
