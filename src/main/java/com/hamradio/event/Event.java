package com.hamradio.event;

public class Event {
    private final long timestamp;
    private final String topic;
    private final Object source;

    public Event(String topic, Object source) {
        this.timestamp = System.currentTimeMillis();
        this.topic = topic;
        this.source = source;
    }

    public long getTimestamp() { return timestamp; }
    public String getTopic() { return topic; }
    public Object getSource() { return source; }
}
