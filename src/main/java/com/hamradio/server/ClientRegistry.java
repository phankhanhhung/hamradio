package com.hamradio.server;

import com.hamradio.protocol.Message;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry of connected clients, keyed by callsign.
 * Provides broadcast helpers for sending messages to all or a subset of clients.
 */
public class ClientRegistry {

    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    /**
     * Registers a client handler under the given callsign.
     * If a handler with the same callsign already exists, it is replaced.
     */
    public void register(String callsign, ClientHandler handler) {
        clients.put(callsign, handler);
    }

    /**
     * Removes the client handler associated with the given callsign.
     */
    public void unregister(String callsign) {
        clients.remove(callsign);
    }

    /**
     * Returns the handler for a specific callsign, or null if not found.
     */
    public ClientHandler getHandler(String callsign) {
        return clients.get(callsign);
    }

    /**
     * Returns a snapshot of all currently connected handlers.
     * The returned collection is safe to iterate without external synchronization.
     */
    public Collection<ClientHandler> getAllHandlers() {
        return clients.values();
    }

    /**
     * Sends a message to every connected client.
     * Errors on individual sends are logged but do not prevent delivery to other clients.
     */
    public void broadcast(Message msg) {
        for (ClientHandler handler : clients.values()) {
            try {
                handler.sendMessage(msg);
            } catch (Exception e) {
                System.err.println("[ClientRegistry] broadcast failed to "
                        + handler.getCallsign() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Sends a message to every connected client except the one with the given callsign.
     */
    public void broadcastExcept(String excludeCallsign, Message msg) {
        for (ClientHandler handler : clients.values()) {
            if (handler.getCallsign() != null && handler.getCallsign().equals(excludeCallsign)) {
                continue;
            }
            try {
                handler.sendMessage(msg);
            } catch (Exception e) {
                System.err.println("[ClientRegistry] broadcastExcept failed to "
                        + handler.getCallsign() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Returns the number of currently registered clients.
     */
    public int getCount() {
        return clients.size();
    }
}
