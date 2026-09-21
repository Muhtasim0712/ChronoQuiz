package com.chronoquiz;

import com.chronoquiz.db.DatabaseManager;
import com.chronoquiz.ui.NavigationManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX Application Entry Point.
 * Sets up primary Stage and displays the initial Start screen.
 */
public class Main extends Application {

    @Override
    public void init() {
        // Pre-initialize SQLite database connection and verify schema on startup
        DatabaseManager.getInstance();
    }

    @Override
    public void start(Stage primaryStage) {
        NavigationManager nav = NavigationManager.getInstance();
        nav.init(primaryStage);

        // Ensure background executor threads terminate when window is closed
        primaryStage.setOnCloseRequest(event -> {
            System.exit(0);
        });

        nav.showStartScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
