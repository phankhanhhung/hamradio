package com.hamradio.event.events;

import com.hamradio.event.Event;

public class LogEvent extends Event {
    private final String level;
    private final String message;

    public LogEvent(Object source, String level, String message) {
        super("log", source);
        this.level = level;
        this.message = message;
    }

    public String getLevel() { return level; }
    public String getMessage() { return message; }
}
