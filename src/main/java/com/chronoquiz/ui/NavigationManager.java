package com.chronoquiz.ui;

import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.Quiz;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * Manages scene routing and stage transitions across the application.
 */
public class NavigationManager {
    private static NavigationManager instance;
    private Stage primaryStage;
    private Scene currentScene;

    private NavigationManager() {
    }

    public static synchronized NavigationManager getInstance() {
        if (instance == null) {
            instance = new NavigationManager();
        }
        return instance;
    }

    public void init(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("ChronoQuiz — Timed Desktop Examination");
        this.primaryStage.setMinWidth(960);
        this.primaryStage.setMinHeight(680);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setRoot(Region root) {
        if (currentScene == null) {
            currentScene = new Scene(root, 1020, 720);
            applyStylesheets(currentScene);
            primaryStage.setScene(currentScene);
        } else {
            currentScene.setRoot(root);
            applyStylesheets(currentScene);
        }
        primaryStage.show();
    }

    public void applyStylesheets(Scene scene) {
        try {
            String css = Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        } catch (Exception e) {
            System.err.println("Could not load style.css: " + e.getMessage());
        }
    }

    public void showStartScreen() {
        StartView view = new StartView();
        setRoot(view.getView());
    }

    public void showQuizScreen(Quiz quiz) {
        QuizView view = new QuizView(quiz);
        setRoot(view.getView());
    }

    public void showResultsScreen(Quiz quiz, Attempt attempt) {
        ResultsView view = new ResultsView(quiz, attempt);
        setRoot(view.getView());
    }

    public void showReviewScreen(Quiz quiz, Attempt attempt) {
        ReviewView view = new ReviewView(quiz, attempt);
        setRoot(view.getView());
    }

    public void showQuestionManagerScreen() {
        QuestionManagerView view = new QuestionManagerView();
        setRoot(view.getView());
    }

    public void showHistoryScreen() {
        HistoryView view = new HistoryView();
        setRoot(view.getView());
    }
}
