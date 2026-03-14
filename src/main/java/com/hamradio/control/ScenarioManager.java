package com.hamradio.control;

import com.hamradio.event.EventBus;
import com.hamradio.event.events.LogEvent;
import com.hamradio.event.events.ScenarioStateEvent;

import java.util.List;

public class ScenarioManager {

    private final EventBus eventBus;
    private final ScenarioValidator validator = new ScenarioValidator();
    private final ResourceAllocator resourceAllocator;

    private ScenarioConfig config;
    private ScenarioState state = ScenarioState.DRAFT;

    public ScenarioManager(EventBus eventBus, ResourceAllocator resourceAllocator) {
        this.eventBus = eventBus;
        this.resourceAllocator = resourceAllocator;
    }

    public void setConfig(ScenarioConfig config) {
        ensureState(ScenarioState.DRAFT);
        this.config = config;
        log("Scenario configured: " + config.getName());
    }

    public void validate() {
        ensureState(ScenarioState.DRAFT);
        List<String> errors = validator.validate(config);
        if (!errors.isEmpty()) {
            transition(ScenarioState.FAILED);
            log("Validation failed: " + String.join(", ", errors));
            throw new IllegalStateException("Validation failed: " + String.join(", ", errors));
        }
        transition(ScenarioState.VALIDATED);
        log("Scenario validated");
    }

    public void load() {
        ensureState(ScenarioState.VALIDATED);
        transition(ScenarioState.LOADING);
        log("Loading scenario...");

        resourceAllocator.initialize();

        transition(ScenarioState.RUNNING);
        log("Scenario running");
    }

    public void pause() {
        ensureState(ScenarioState.RUNNING);
        transition(ScenarioState.PAUSED);
        log("Scenario paused");
    }

    public void resume() {
        ensureState(ScenarioState.PAUSED);
        transition(ScenarioState.RUNNING);
        log("Scenario resumed");
    }

    public void complete() {
        if (state != ScenarioState.RUNNING && state != ScenarioState.PAUSED) {
            throw new IllegalStateException("Cannot complete from state: " + state);
        }
        transition(ScenarioState.COMPLETED);
        resourceAllocator.shutdown();
        log("Scenario completed");
    }

    public void fail(String reason) {
        transition(ScenarioState.FAILED);
        resourceAllocator.shutdown();
        log("Scenario failed: " + reason);
    }

    public void reset() {
        transition(ScenarioState.DRAFT);
        config = null;
        log("Scenario reset");
    }

    public ScenarioState getState() { return state; }
    public ScenarioConfig getConfig() { return config; }

    private void transition(ScenarioState newState) {
        ScenarioState old = this.state;
        this.state = newState;
        eventBus.publish(new ScenarioStateEvent(this, old.name(), newState.name()));
    }

    private void ensureState(ScenarioState expected) {
        if (this.state != expected) {
            throw new IllegalStateException("Expected state " + expected + " but was " + state);
        }
    }

    private void log(String msg) {
        eventBus.publish(new LogEvent(this, "INFO", "[ScenarioManager] " + msg));
    }
}
