package com.hamradio.event;

@FunctionalInterface
public interface EventListener {
    void onEvent(Event event);
}
