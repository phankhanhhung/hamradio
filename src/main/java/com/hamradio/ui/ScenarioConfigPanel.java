package com.hamradio.ui;

import com.hamradio.control.ScenarioConfig;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class ScenarioConfigPanel extends GridPane {

    private final TextField nameField = new TextField("HF SSB QSO");
    private final ComboBox<String> propagationCombo = new ComboBox<>();
    private final Spinner<Integer> sampleRateSpinner;
    private final Spinner<Double> durationSpinner;

    // Station 1
    private final TextField call1Field = new TextField("VK3ABC");
    private final TextField freq1Field = new TextField("7100000");
    private final TextField lat1Field = new TextField("-37.8");
    private final TextField lon1Field = new TextField("144.9");

    // Station 2
    private final TextField call2Field = new TextField("W1XYZ");
    private final TextField freq2Field = new TextField("7100000");
    private final TextField lat2Field = new TextField("42.3");
    private final TextField lon2Field = new TextField("-71.0");

    private final ComboBox<String> modeCombo = new ComboBox<>();

    public ScenarioConfigPanel() {
        setHgap(8);
        setVgap(6);
        setPadding(new Insets(10));

        propagationCombo.getItems().addAll("fspl", "multipath", "ionospheric", "full");
        propagationCombo.setValue("full");

        modeCombo.getItems().addAll("AM", "FM", "SSB");
        modeCombo.setValue("SSB");

        sampleRateSpinner = new Spinner<>(8000, 192000, 44100, 4000);
        sampleRateSpinner.setEditable(true);
        sampleRateSpinner.setPrefWidth(100);

        durationSpinner = new Spinner<>(1.0, 3600.0, 30.0, 5.0);
        durationSpinner.setEditable(true);
        durationSpinner.setPrefWidth(100);

        int row = 0;
        addFormRow(row++, lbl("Scenario:"), nameField);
        addFormRow(row++, lbl("Propagation:"), propagationCombo);
        addFormRow(row++, lbl("Mode:"), modeCombo);
        addFormRow(row++, lbl("Sample Rate:"), sampleRateSpinner);
        addFormRow(row++, lbl("Duration (s):"), durationSpinner);
        addFormRow(row++, new Separator(), new Separator());
        addFormRow(row++, lbl("Station 1"), lbl(""));
        addFormRow(row++, lbl("Callsign:"), call1Field);
        addFormRow(row++, lbl("Freq (Hz):"), freq1Field);
        addFormRow(row++, lbl("Lat/Lon:"), lat1Field, lon1Field);
        addFormRow(row++, new Separator(), new Separator());
        addFormRow(row++, lbl("Station 2"), lbl(""));
        addFormRow(row++, lbl("Callsign:"), call2Field);
        addFormRow(row++, lbl("Freq (Hz):"), freq2Field);
        addFormRow(row++, lbl("Lat/Lon:"), lat2Field, lon2Field);
    }

    public ScenarioConfig buildConfig() {
        ScenarioConfig config = new ScenarioConfig(nameField.getText());
        config.setPropagationModel(propagationCombo.getValue());
        config.setSampleRate(sampleRateSpinner.getValue());
        config.setDurationSeconds(durationSpinner.getValue());

        String mode = modeCombo.getValue();

        ScenarioConfig.StationConfig s1 = new ScenarioConfig.StationConfig(
                call1Field.getText(),
                Double.parseDouble(lat1Field.getText()),
                Double.parseDouble(lon1Field.getText()),
                Double.parseDouble(freq1Field.getText()),
                mode
        );
        config.addStation(s1);

        ScenarioConfig.StationConfig s2 = new ScenarioConfig.StationConfig(
                call2Field.getText(),
                Double.parseDouble(lat2Field.getText()),
                Double.parseDouble(lon2Field.getText()),
                Double.parseDouble(freq2Field.getText()),
                mode
        );
        config.addStation(s2);

        return config;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: lightgray;");
        return l;
    }

    private void addFormRow(int row, javafx.scene.Node... nodes) {
        for (int i = 0; i < nodes.length; i++) {
            add(nodes[i], i, row);
        }
    }
}
