package com.hamradio.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Main entry point for the HamRadio simulation server.
 * <p>
 * Accepts TCP connections, enforces the max-clients limit, and spawns a
 * {@link ClientHandler} thread for each accepted connection.  The server
 * can be stopped gracefully via {@link #stop()} (also wired as a JVM
 * shutdown hook).
 */
public class HamRadioServer {

    private final ServerConfig config;
    private final ClientRegistry registry = new ClientRegistry();
    private final SimulationEngine engine;
    private volatile boolean running;
    private ServerSocket serverSocket;

    public HamRadioServer(ServerConfig config) {
        this.config = config;
        this.engine = new SimulationEngine(registry, config.getPropagationModel(), config.getSampleRate());
    }

    /**
     * Starts the server: opens the listening socket and enters the accept loop.
     * This method blocks until {@link #stop()} is called or an unrecoverable error occurs.
     */
    public void start() throws Exception {
        running = true;

        System.out.println("========================================");
        System.out.println("  HamRadio Simulation Server");
        System.out.println("========================================");
        System.out.println("  Port:              " + config.getPort());
        System.out.println("  Sample rate:       " + config.getSampleRate() + " Hz");
        System.out.println("  Propagation model: " + config.getPropagationModel());
        System.out.println("  Max clients:       " + config.getMaxClients());
        System.out.println("========================================");

        serverSocket = new ServerSocket(config.getPort());
        System.out.println("[Server] Listening on port " + config.getPort() + "...");

        while (running) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (SocketException e) {
                // serverSocket was closed by stop() -- normal shutdown
                if (!running) {
                    break;
                }
                throw e;
            }

            // Check max-clients limit
            if (registry.getCount() >= config.getMaxClients()) {
                System.err.println("[Server] Rejecting connection from "
                        + clientSocket.getRemoteSocketAddress()
                        + " -- max clients (" + config.getMaxClients() + ") reached");
                try {
                    // Send a CONNECT_NACK before closing, if possible
                    com.hamradio.protocol.messages.ConnectNackMessage nack =
                            new com.hamradio.protocol.messages.ConnectNackMessage(
                                    "Server full: " + config.getMaxClients() + " clients connected");
                    com.hamradio.protocol.MessageCodec.write(clientSocket.getOutputStream(), nack);
                } catch (IOException ignored) {
                    // Best-effort
                }
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                    // Ignore
                }
                continue;
            }

            System.out.println("[Server] Accepted connection from "
                    + clientSocket.getRemoteSocketAddress());

            ClientHandler handler = new ClientHandler(clientSocket, engine, registry, config);
            Thread handlerThread = new Thread(handler, "client-" + clientSocket.getRemoteSocketAddress());
            handlerThread.setDaemon(true);
            handlerThread.start();
        }

        System.out.println("[Server] Accept loop exited.");
    }

    /**
     * Signals the server to stop.  Closes the server socket (unblocking accept()),
     * disconnects all clients, and shuts down the simulation engine.
     */
    public void stop() {
        running = false;

        // Close the server socket to interrupt accept()
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("[Server] Error closing server socket: " + e.getMessage());
            }
        }

        // Close all connected client handlers
        for (ClientHandler handler : registry.getAllHandlers()) {
            try {
                handler.close();
            } catch (Exception e) {
                System.err.println("[Server] Error closing client "
                        + handler.getCallsign() + ": " + e.getMessage());
            }
        }

        // Shut down the DSP engine
        try {
            engine.shutdown();
        } catch (Exception e) {
            System.err.println("[Server] Error shutting down engine: " + e.getMessage());
        }

        System.out.println("[Server] Shutdown complete.");
    }

    /**
     * CLI entry point.  Parses arguments, installs a shutdown hook, and starts the server.
     */
    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.parse(args);
        HamRadioServer server = new HamRadioServer(config);

        // Install JVM shutdown hook for graceful stop on Ctrl-C / SIGTERM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Server] Shutdown hook triggered...");
            server.stop();
        }, "server-shutdown"));

        server.start();
    }
}
