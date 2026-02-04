package com.hamradio.event;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<EventListener>> subscribers =
            new ConcurrentHashMap<>();

    public EventBus() {}

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public void subscribe(String topic, EventListener listener) {
        subscribers.computeIfAbsent(topic, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void unsubscribe(String topic, EventListener listener) {
        CopyOnWriteArrayList<EventListener> listeners = subscribers.get(topic);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    public void publish(Event event) {
        CopyOnWriteArrayList<EventListener> listeners = subscribers.get(event.getTopic());
        if (listeners != null) {
            for (EventListener listener : listeners) {
                listener.onEvent(event);
            }
        }
    }
}
