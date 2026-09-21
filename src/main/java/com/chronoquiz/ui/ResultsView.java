package com.chronoquiz.ui;

import com.chronoquiz.db.AttemptDAO;
import com.chronoquiz.db.UserDAO;
import com.chronoquiz.model.*;
import com.chronoquiz.util.JsonExporter;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Instant Scoring & Results Screen:
 * Calculates performance metrics, saves attempt to SQLite concurrently,
 * and allows exporting results to JSON or reviewing answers.
 */
public class ResultsView {
    private final BorderPane root = new BorderPane();
    private final Quiz quiz;
    private Attempt attempt;
    private final AttemptDAO attemptDAO = new AttemptDAO();
    private final UserDAO userDAO = new UserDAO();
    private final JsonExporter jsonExporter = new JsonExporter();

    private Label savingStatusLabel;

    public ResultsView(Quiz quiz, Attempt existingAttempt) {
        this.quiz = quiz;
        this.attempt = existingAttempt;

        buildUI();

        if (this.attempt == null && this.quiz != null) {
            saveAttemptConcurrently();
        }
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(30, 40, 30, 40));

        // Header
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);

        Label headerLbl = new Label("Quiz Complete!");
        headerLbl.getStyleClass().add("title-large");

        Label playerLbl = new Label("Performance Summary for: " + (quiz != null ? quiz.getUserName() : (attempt != null ? attempt.getUserName() : "Player")));
        playerLbl.getStyleClass().add("subtitle");

        headerBox.getChildren().addAll(headerLbl, playerLbl);
        root.setTop(headerBox);

        // Center Card
        VBox centerCard = new VBox(24);
        centerCard.getStyleClass().add("card-accent");
        centerCard.setAlignment(Pos.CENTER);
        centerCard.setMaxWidth(680);

        int score = quiz != null ? quiz.calculateScore() : (attempt != null ? attempt.getScore() : 0);
        int total = quiz != null ? quiz.getTotalQuestions() : (attempt != null ? attempt.getTotal() : 0);
        double percentage = total > 0 ? ((double) score / total) * 100.0 : 0.0;
        int timeTaken = quiz != null ? quiz.getTimeTakenSeconds() : (attempt != null ? attempt.getTimeTakenSec() : 0);

        // Performance Badge
        Label badgeLabel = new Label();
        badgeLabel.getStyleClass().add("badge");
        if (percentage >= 80.0) {
            badgeLabel.setText("EXCELLENT — Top Tier Mastery");
            badgeLabel.getStyleClass().add("badge-easy");
        } else if (percentage >= 50.0) {
            badgeLabel.setText("GOOD EFFORT — Solid Knowledge");
            badgeLabel.getStyleClass().add("badge-medium");
        } else {
            badgeLabel.setText("NEEDS PRACTICE — Review Below");
            badgeLabel.getStyleClass().add("badge-hard");
        }

        // Score display
        Label scoreVal = new Label(score + " / " + total);
        scoreVal.setStyle("-fx-font-size: 52px; -fx-font-weight: 900; -fx-text-fill: #6366f1;");

        Label pctVal = new Label(String.format("%.1f%% Correct", percentage));
        pctVal.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #f8fafc;");

        // Metrics Grid
        GridPane metricsGrid = new GridPane();
        metricsGrid.setHgap(20);
        metricsGrid.setVgap(12);
        metricsGrid.setAlignment(Pos.CENTER);

        metricsGrid.add(createMetricBox("Correct Answers", String.valueOf(score), "#10b981"), 0, 0);
        metricsGrid.add(createMetricBox("Incorrect Answers", String.valueOf(total - score), "#ef4444"), 1, 0);
        metricsGrid.add(createMetricBox("Time Taken", formatTime(timeTaken), "#38bdf8"), 2, 0);

        savingStatusLabel = new Label("Saving attempt to database in background...");
        savingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        centerCard.getChildren().addAll(badgeLabel, scoreVal, pctVal, metricsGrid, savingStatusLabel);

        StackPane centerWrapper = new StackPane(centerCard);
        centerWrapper.setAlignment(Pos.CENTER);
        root.setCenter(centerWrapper);

        // Bottom Actions
        HBox bottomBox = new HBox(16);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(20, 0, 10, 0));

        Button reviewBtn = new Button("Review All Answers");
        reviewBtn.getStyleClass().addAll("button", "button-primary");
        reviewBtn.setOnAction(e -> NavigationManager.getInstance().showReviewScreen(quiz, attempt));

        Button exportJsonBtn = new Button("Export Result (JSON)");
        exportJsonBtn.getStyleClass().addAll("button", "button-outline");
        exportJsonBtn.setOnAction(e -> handleExportResult());

        Button retakeBtn = new Button("Play Again");
        retakeBtn.getStyleClass().addAll("button", "button-success");
        retakeBtn.setOnAction(e -> NavigationManager.getInstance().showStartScreen());

        Button menuBtn = new Button("Main Menu");
        menuBtn.getStyleClass().addAll("button", "button-outline");
        menuBtn.setOnAction(e -> NavigationManager.getInstance().showStartScreen());

        bottomBox.getChildren().addAll(reviewBtn, exportJsonBtn, retakeBtn, menuBtn);
        root.setBottom(bottomBox);
    }

    private VBox createMetricBox(String labelText, String valueText, String hexColor) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("card-subtle");
        box.setPrefWidth(160);

        Label val = new Label(valueText);
        val.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + hexColor + ";");

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        box.getChildren().addAll(val, lbl);
        return box;
    }

    private void saveAttemptConcurrently() {
        CompletableFuture.runAsync(() -> {
            try {
                User user = userDAO.getOrCreateUser(quiz.getUserName());
                int score = quiz.calculateScore();
                int total = quiz.getTotalQuestions();
                int timeTaken = quiz.getTimeTakenSeconds();
                String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                Attempt newAttempt = new Attempt(
                        0,
                        user.getId(),
                        user.getName(),
                        quiz.getCategoryId(),
                        quiz.getCategoryName(),
                        score,
                        total,
                        timeTaken,
                        now
                );

                List<AttemptAnswer> answers = new ArrayList<>();
                for (Question q : quiz.getQuestions()) {
                    answers.add(new AttemptAnswer(
                            q.getId(),
                            q.getText(),
                            q.getUserAnswer() != null ? q.getUserAnswer() : "[Unanswered]",
                            q.getCorrectAnswerDisplay(),
                            q.isCorrect()
                    ));
                }
                newAttempt.setAnswers(answers);

                boolean ok = attemptDAO.saveAttempt(newAttempt);
                this.attempt = newAttempt;

                Platform.runLater(() -> {
                    if (ok) {
                        savingStatusLabel.setText("✓ Attempt saved to permanent SQLite history.");
                        savingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #10b981; -fx-font-weight: bold;");
                    } else {
                        savingStatusLabel.setText("⚠ Warning: Could not save attempt to SQLite.");
                        savingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444;");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    savingStatusLabel.setText("⚠ Error saving attempt: " + ex.getMessage());
                    savingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444;");
                });
            }
        });
    }

    private void handleExportResult() {
        if (attempt == null && quiz != null) {
            attempt = new Attempt(
                    0, 1, quiz.getUserName(), quiz.getCategoryId(), quiz.getCategoryName(),
                    quiz.calculateScore(), quiz.getTotalQuestions(), quiz.getTimeTakenSeconds(),
                    LocalDateTime.now().toString()
            );
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Quiz Result");
        fileChooser.setInitialFileName("chronoquiz_result_" + System.currentTimeMillis() + ".json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showSaveDialog(NavigationManager.getInstance().getPrimaryStage());
        if (file != null) {
            try {
                jsonExporter.exportAttemptToFile(attempt, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Succeeded");
                alert.setHeaderText(null);
                alert.setContentText("Quiz result exported successfully to:\n" + file.getAbsolutePath());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Failed to export results: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }

    private String formatTime(int totalSeconds) {
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
