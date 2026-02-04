package com.hamradio.event.events;

import com.hamradio.event.Event;

public class ScenarioStateEvent extends Event {
    private final String oldState;
    private final String newState;

    public ScenarioStateEvent(Object source, String oldState, String newState) {
        super("scenario.state", source);
        this.oldState = oldState;
        this.newState = newState;
    }

    public String getOldState() { return oldState; }
    public String getNewState() { return newState; }
}
