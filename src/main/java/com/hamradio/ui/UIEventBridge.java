package com.hamradio.ui;

import com.hamradio.event.Event;
import com.hamradio.event.EventBus;
import com.hamradio.event.events.*;
import javafx.application.Platform;

import java.util.function.Consumer;

public class UIEventBridge {

    private final EventBus eventBus;
    private Consumer<LogEvent> onLog;
    private Consumer<ScenarioStateEvent> onScenarioState;
    private Consumer<SpectrumDataEvent> onSpectrumData;
    private Consumer<StationEvent> onStation;
    private Consumer<SignalEvent> onSignal;

    public UIEventBridge(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void bind() {
        eventBus.subscribe("log", e -> dispatch(e, onLog, LogEvent.class));
        eventBus.subscribe("scenario.state", e -> dispatch(e, onScenarioState, ScenarioStateEvent.class));
        eventBus.subscribe("spectrum.data", e -> dispatch(e, onSpectrumData, SpectrumDataEvent.class));
        eventBus.subscribe("station", e -> dispatch(e, onStation, StationEvent.class));
        eventBus.subscribe("signal", e -> dispatch(e, onSignal, SignalEvent.class));
    }

    @SuppressWarnings("unchecked")
    private <T extends Event> void dispatch(Event event, Consumer<T> handler, Class<T> type) {
        if (handler != null && type.isInstance(event)) {
            Platform.runLater(() -> handler.accept((T) event));
        }
    }

    public void setOnLog(Consumer<LogEvent> h) { this.onLog = h; }
    public void setOnScenarioState(Consumer<ScenarioStateEvent> h) { this.onScenarioState = h; }
    public void setOnSpectrumData(Consumer<SpectrumDataEvent> h) { this.onSpectrumData = h; }
    public void setOnStation(Consumer<StationEvent> h) { this.onStation = h; }
    public void setOnSignal(Consumer<SignalEvent> h) { this.onSignal = h; }
}
