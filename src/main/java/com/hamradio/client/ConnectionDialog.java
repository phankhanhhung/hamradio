package com.hamradio.client;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

/**
 * JavaFX dialog that collects server connection and station configuration
 * parameters from the user before connecting to a HamRadio server.
 */
public class ConnectionDialog extends Dialog<ConnectionDialog.ConnectionInfo> {

    /**
     * Immutable record of all connection and station parameters.
     */
    public static class ConnectionInfo {
        public final String host;
        public final int port;
        public final String callsign;
        public final double latitude;
        public final double longitude;
        public final double frequencyHz;
        public final String mode;
        public final boolean upperSideband;

        public ConnectionInfo(String host, int port, String callsign,
                              double latitude, double longitude,
                              double frequencyHz, String mode, boolean upperSideband) {
            this.host = host;
            this.port = port;
            this.callsign = callsign;
            this.latitude = latitude;
            this.longitude = longitude;
            this.frequencyHz = frequencyHz;
            this.mode = mode;
            this.upperSideband = upperSideband;
        }
    }

    private final TextField hostField = new TextField("localhost");
    private final TextField portField = new TextField("7100");
    private final TextField callsignField = new TextField("VK3ABC");
    private final TextField latitudeField = new TextField("-37.8");
    private final TextField longitudeField = new TextField("144.9");
    private final TextField frequencyField = new TextField("7100000");
    private final ComboBox<String> modeBox = new ComboBox<>();
    private final CheckBox usbCheckBox = new CheckBox("Upper Sideband");

    public ConnectionDialog() {
        setTitle("Connect to HamRadio Server");
        setHeaderText("Enter station details");

        // Mode combo box
        modeBox.getItems().addAll("AM", "FM", "SSB");
        modeBox.setValue("SSB");

        // USB defaults to true
        usbCheckBox.setSelected(true);

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(20, 150, 10, 10));

        int row = 0;
        grid.add(new Label("Host:"), 0, row);
        grid.add(hostField, 1, row++);

        grid.add(new Label("Port:"), 0, row);
        grid.add(portField, 1, row++);

        grid.add(new Label("Callsign:"), 0, row);
        grid.add(callsignField, 1, row++);

        grid.add(new Label("Latitude:"), 0, row);
        grid.add(latitudeField, 1, row++);

        grid.add(new Label("Longitude:"), 0, row);
        grid.add(longitudeField, 1, row++);

        grid.add(new Label("Frequency (Hz):"), 0, row);
        grid.add(frequencyField, 1, row++);

        grid.add(new Label("Mode:"), 0, row);
        grid.add(modeBox, 1, row++);

        grid.add(usbCheckBox, 1, row);

        getDialogPane().setContent(grid);

        // Buttons
        ButtonType connectButtonType = new ButtonType("Connect", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);

        // Result converter
        setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                try {
                    String host = hostField.getText().trim();
                    int port = Integer.parseInt(portField.getText().trim());
                    String callsign = callsignField.getText().trim().toUpperCase();
                    double latitude = Double.parseDouble(latitudeField.getText().trim());
                    double longitude = Double.parseDouble(longitudeField.getText().trim());
                    double frequencyHz = Double.parseDouble(frequencyField.getText().trim());
                    String mode = modeBox.getValue();
                    boolean usb = usbCheckBox.isSelected();
                    return new ConnectionInfo(host, port, callsign, latitude, longitude,
                            frequencyHz, mode, usb);
                } catch (NumberFormatException e) {
                    // Return null on parse errors — caller should handle
                    return null;
                }
            }
            return null;
        });
    }
}
