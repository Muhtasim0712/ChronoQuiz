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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Modern Engaging Results Dashboard:
 * Prominently presents final scores, performance summary, individual KPI cards
 * for correct, incorrect, unanswered, and time taken, and provides intuitive actions.
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
        root.setPadding(new Insets(24, 40, 26, 40));

        // Top Navigation Bar with Back Button
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Main Menu");
        backBtn.getStyleClass().addAll("button", "button-outline");
        backBtn.setOnAction(e -> NavigationManager.getInstance().showStartScreen());

        VBox titleBox = new VBox(2);
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label trophyIcon = new Label("🏆");
        trophyIcon.setStyle("-fx-font-size: 26px;");
        Label headerLbl = new Label("Examination Evaluation Complete");
        headerLbl.getStyleClass().add("title-large");
        headerLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: 900; -fx-text-fill: #ffffff;");
        titleRow.getChildren().addAll(trophyIcon, headerLbl);

        String studentName = quiz != null ? quiz.getUserName() : (attempt != null ? attempt.getUserName() : "Student");
        Label playerLbl = new Label("Performance Report for: " + studentName);
        playerLbl.getStyleClass().add("subtitle");
        playerLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #38bdf8; -fx-font-weight: 600;");
        titleBox.getChildren().addAll(titleRow, playerLbl);

        topBox.getChildren().addAll(backBtn, titleBox);
        root.setTop(topBox);

        // Center Hero Card
        VBox centerCard = new VBox(22);
        centerCard.getStyleClass().add("card-hero");
        centerCard.setAlignment(Pos.CENTER);
        centerCard.setMaxWidth(760);

        int score = quiz != null ? quiz.calculateScore() : (attempt != null ? attempt.getScore() : 0);
        int total = quiz != null ? quiz.getTotalQuestions() : (attempt != null ? attempt.getTotal() : 0);
        double percentage = total > 0 ? ((double) score / total) * 100.0 : 0.0;
        int timeTaken = quiz != null ? quiz.getTimeTakenSeconds() : (attempt != null ? attempt.getTimeTakenSec() : 0);

        // Count unanswered questions
        int unansweredCount = 0;
        if (quiz != null) {
            for (Question q : quiz.getQuestions()) {
                if (q.getUserAnswer() == null || q.getUserAnswer().trim().isEmpty()) {
                    unansweredCount++;
                }
            }
        } else if (attempt != null && attempt.getAnswers() != null) {
            for (AttemptAnswer a : attempt.getAnswers()) {
                if (a.getUserAnswer() == null || a.getUserAnswer().contains("[Unanswered]") || a.getUserAnswer().contains("[No Answer")) {
                    unansweredCount++;
                }
            }
        }
        int incorrectCount = Math.max(0, total - score - unansweredCount);

        // Performance Summary Badge
        Label badgeLabel = new Label();
        badgeLabel.getStyleClass().add("badge");
        if (percentage >= 80.0) {
            badgeLabel.setText("★ EXCELLENT MASTERY — Top Tier Academic Performance!");
            badgeLabel.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399; -fx-border-color: #10b981; -fx-border-radius: 20px; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6px 16px;");
        } else if (percentage >= 50.0) {
            badgeLabel.setText("★ SOLID PROFICIENCY — Good Competency Demonstrated!");
            badgeLabel.setStyle("-fx-background-color: rgba(6, 182, 212, 0.2); -fx-text-fill: #38bdf8; -fx-border-color: #06b6d4; -fx-border-radius: 20px; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6px 16px;");
        } else {
            badgeLabel.setText("★ REVIEW RECOMMENDED — Focus on Weak Areas to Improve!");
            badgeLabel.setStyle("-fx-background-color: rgba(244, 63, 94, 0.2); -fx-text-fill: #fb7185; -fx-border-color: #f43f5e; -fx-border-radius: 20px; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 6px 16px;");
        }

        // Hero Score Display Box
        VBox scoreHeroBox = new VBox(4);
        scoreHeroBox.setAlignment(Pos.CENTER);
        scoreHeroBox.setPadding(new Insets(12, 24, 12, 24));
        scoreHeroBox.setStyle("-fx-background-color: rgba(99, 102, 241, 0.1); -fx-background-radius: 18px; -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-radius: 18px; -fx-border-width: 1px;");

        Label scoreVal = new Label(score + " / " + total);
        scoreVal.setStyle("-fx-font-size: 54px; -fx-font-weight: 900; -fx-text-fill: linear-gradient(to right, #ffffff, #c7d2fe, #38bdf8);");

        Label pctVal = new Label(String.format("%.1f%% Overall Accuracy", percentage));
        pctVal.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #e2e8f0;");

        scoreHeroBox.getChildren().addAll(scoreVal, pctVal);

        // Separate KPI Metric Cards in a Responsive Row
        HBox metricsRow = new HBox(14);
        metricsRow.setAlignment(Pos.CENTER);

        VBox correctCard = createMetricCard("🎯 Correct", String.valueOf(score), "#34d399", "rgba(16, 185, 129, 0.15)");
        VBox incorrectCard = createMetricCard("❌ Incorrect", String.valueOf(incorrectCount), "#fb7185", "rgba(244, 63, 94, 0.15)");
        VBox unansweredCard = createMetricCard("⚪ Unanswered", String.valueOf(unansweredCount), "#fbbf24", "rgba(245, 158, 11, 0.15)");
        VBox timeCard = createMetricCard("⏱ Time Taken", formatTime(timeTaken), "#38bdf8", "rgba(6, 182, 212, 0.15)");

        metricsRow.getChildren().addAll(correctCard, incorrectCard, unansweredCard, timeCard);
        HBox.setHgrow(correctCard, Priority.ALWAYS);
        HBox.setHgrow(incorrectCard, Priority.ALWAYS);
        HBox.setHgrow(unansweredCard, Priority.ALWAYS);
        HBox.setHgrow(timeCard, Priority.ALWAYS);

        savingStatusLabel = new Label("Saving evaluation to database in background...");
        savingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        centerCard.getChildren().addAll(badgeLabel, scoreHeroBox, metricsRow, savingStatusLabel);

        StackPane centerWrapper = new StackPane(centerCard);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setPadding(new Insets(14, 0, 14, 0));

        ScrollPane scrollWrapper = new ScrollPane(centerWrapper);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setStyle("-fx-background-color: transparent;");

        root.setCenter(scrollWrapper);

        // Bottom Actions Row (without Try Again button)
        HBox bottomBox = new HBox(16);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(12, 0, 8, 0));

        Button reviewBtn = new Button("Review Answers  📝");
        reviewBtn.getStyleClass().addAll("button", "button-primary");
        reviewBtn.setOnAction(e -> NavigationManager.getInstance().showReviewScreen(quiz, attempt, false));

        Button exportJsonBtn = new Button("Export Result (JSON)  📥");
        exportJsonBtn.getStyleClass().addAll("button", "button-outline");
        exportJsonBtn.setOnAction(e -> handleExportResult());

        Button menuBtn = new Button("Exit to Main Menu  🏠");
        menuBtn.getStyleClass().addAll("button", "button-outline");
        menuBtn.setOnAction(e -> NavigationManager.getInstance().showStartScreen());

        bottomBox.getChildren().addAll(reviewBtn, exportJsonBtn, menuBtn);
        root.setBottom(bottomBox);
    }

    private VBox createMetricCard(String labelText, String valueText, String hexColor, String bgTint) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("card-subtle");
        box.setStyle("-fx-background-color: " + bgTint + "; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 14px; -fx-background-radius: 14px;");
        box.setPadding(new Insets(14, 12, 14, 12));
        box.setMinWidth(130);

        Label val = new Label(valueText);
        val.setStyle("-fx-font-size: 26px; -fx-font-weight: 900; -fx-text-fill: " + hexColor + ";");

        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #e2e8f0;");

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
                        savingStatusLabel.setText("✓ Evaluation recorded in permanent SQLite database.");
                        savingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #34d399; -fx-font-weight: bold;");
                    } else {
                        savingStatusLabel.setText("⚠ Warning: Could not save attempt to SQLite.");
                        savingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #fb7185;");
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    savingStatusLabel.setText("⚠ Error saving attempt: " + ex.getMessage());
                    savingStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #fb7185;");
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
