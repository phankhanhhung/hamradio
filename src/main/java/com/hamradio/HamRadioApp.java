package com.hamradio;

import com.hamradio.ui.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class HamRadioApp extends Application {

    private MainWindow mainWindow;

    @Override
    public void start(Stage primaryStage) {
        mainWindow = new MainWindow(primaryStage);
        mainWindow.build();

        primaryStage.setOnCloseRequest(e -> {
            mainWindow.shutdown();
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
