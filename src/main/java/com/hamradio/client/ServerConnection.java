package com.hamradio.client;

import com.hamradio.protocol.Message;
import com.hamradio.protocol.MessageCodec;
import com.hamradio.protocol.messages.DisconnectMessage;

import java.io.*;
import java.net.Socket;

/**
 * Manages the TCP socket connection to a HamRadio server.
 * Runs a background reader thread that dispatches incoming messages
 * to the {@link MessageDispatcher}.
 */
public class ServerConnection {

    private Socket socket;
    private OutputStream out;
    private InputStream in;
    private Thread readerThread;
    private volatile boolean connected;
    private final MessageDispatcher dispatcher;

    public ServerConnection(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Connects to the server at the specified host and port.
     * Starts a daemon reader thread to process incoming messages.
     *
     * @param host server hostname or IP
     * @param port server TCP port
     * @throws IOException if the connection fails
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        socket.setTcpNoDelay(true);
        out = socket.getOutputStream();
        in = socket.getInputStream();
        connected = true;

        readerThread = new Thread(this::readerLoop, "server-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    /**
     * Sends a message to the server. Thread-safe via synchronization.
     *
     * @param msg the message to send
     * @throws IOException if writing fails
     */
    public synchronized void sendMessage(Message msg) throws IOException {
        if (connected && out != null) {
            MessageCodec.write(out, msg);
        }
    }

    /**
     * Background loop that reads messages from the server and dispatches them.
     * Exits on IOException (disconnection) and notifies the dispatcher.
     */
    private void readerLoop() {
        try {
            while (connected) {
                Message msg = MessageCodec.read(in);
                dispatcher.dispatch(msg);
            }
        } catch (IOException e) {
            if (connected) {
                connected = false;
                dispatcher.onDisconnected(e.getMessage());
            }
        }
    }

    /**
     * Gracefully disconnects from the server.
     * Sends a DISCONNECT message before closing the socket.
     */
    public void disconnect() {
        if (!connected) return;
        connected = false;

        // Send disconnect message (best-effort)
        try {
            if (out != null) {
                MessageCodec.write(out, new DisconnectMessage());
            }
        } catch (IOException ignored) {
            // Socket may already be closed
        }

        // Close socket
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {
            // Best-effort close
        }

        // Interrupt reader thread if still alive
        if (readerThread != null && readerThread.isAlive()) {
            readerThread.interrupt();
        }
    }

    /**
     * Returns whether the connection is currently active.
     */
    public boolean isConnected() {
        return connected;
    }
}
