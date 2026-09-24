package com.chronoquiz.ui;

import com.chronoquiz.model.*;
import com.chronoquiz.service.QuizTimerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Modern Focused Quiz Screen:
 * Displays questions inside a distraction-free student evaluation card,
 * with elevated interactive answer options, dynamic urgency-indicating timer capsule,
 * and a smooth gradient progress bar.
 */
public class QuizView {
    private final BorderPane root = new BorderPane();
    private final Quiz quiz;
    private final QuizTimerService timerService;

    private HBox timerCapsule;
    private Label timerIconLabel;
    private Label timerLabel;
    private ProgressBar progressBar;
    private Label progressTextLabel;
    private Label questionBadgeLabel;
    private Label questionTextLabel;
    private VBox answerContainer;
    private Button nextButton;

    // Multiple Choice state
    private ToggleGroup mcqToggleGroup;

    // Short Answer state
    private TextField shortAnswerField;

    public QuizView(Quiz quiz) {
        this.quiz = quiz;
        this.timerService = new QuizTimerService(quiz.getTotalTimeSeconds());

        buildUI();
        setupTimer();
        renderCurrentQuestion();
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(24, 40, 26, 40));

        // --- TOP SECTION: Metadata, Urgency Timer & Progress Bar ---
        VBox topBox = new VBox(14);

        HBox metaRow = new HBox(14);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label("📚 " + quiz.getCategoryName());
        catBadge.getStyleClass().addAll("badge", "badge-source");

        Label diffBadge = new Label("⚡ " + quiz.getDifficulty().getDisplayName());
        String diffClass = switch (quiz.getDifficulty()) {
            case EASY -> "badge-easy";
            case HARD -> "badge-hard";
            default -> "badge-medium";
        };
        diffBadge.getStyleClass().addAll("badge", diffClass);

        Label studentBadge = new Label("👤 " + quiz.getUserName());
        studentBadge.getStyleClass().addAll("badge", "badge-locked");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // Dynamic Timer Capsule (Solid white background with bold black numbers for maximum visibility)
        timerCapsule = new HBox(8);
        timerCapsule.setAlignment(Pos.CENTER);
        timerCapsule.getStyleClass().addAll("timer-capsule", "timer-normal");
        timerCapsule.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 30px; -fx-padding: 6px 20px;");

        timerIconLabel = new Label("⏱");
        timerIconLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #000000; -fx-fill: #000000;");

        timerLabel = new Label(timerService.getFormattedTime());
        timerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #000000; -fx-fill: #000000;");

        timerCapsule.getChildren().addAll(timerIconLabel, timerLabel);

        metaRow.getChildren().addAll(catBadge, diffBadge, studentBadge, spacer1, timerCapsule);

        // Progress row
        HBox progressRow = new HBox(14);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        progressTextLabel = new Label("Question 1 of " + quiz.getTotalQuestions());
        progressTextLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");

        progressBar = new ProgressBar(1.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().addAll("progress-bar", "progress-normal");
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        progressRow.getChildren().addAll(progressTextLabel, progressBar);

        topBox.getChildren().addAll(metaRow, progressRow);
        root.setTop(topBox);

        // --- CENTER SECTION: Focused Question Card ---
        VBox centerCard = new VBox(22);
        centerCard.getStyleClass().add("card-hero");
        centerCard.setAlignment(Pos.TOP_LEFT);
        centerCard.setMaxWidth(880);

        // Question tag badge
        questionBadgeLabel = new Label("QUESTION 1");
        questionBadgeLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-padding: 3px 10px; -fx-background-color: rgba(6, 182, 212, 0.15); -fx-background-radius: 12px;");

        questionTextLabel = new Label();
        questionTextLabel.setWrapText(true);
        questionTextLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #ffffff; -fx-line-spacing: 4px;");
        questionTextLabel.setMinHeight(50);

        answerContainer = new VBox(14);
        answerContainer.setAlignment(Pos.CENTER_LEFT);

        centerCard.getChildren().addAll(questionBadgeLabel, questionTextLabel, answerContainer);

        StackPane centerWrapper = new StackPane(centerCard);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setPadding(new Insets(16, 0, 16, 0));

        ScrollPane scrollWrapper = new ScrollPane(centerWrapper);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setStyle("-fx-background-color: transparent;");

        root.setCenter(scrollWrapper);

        // --- BOTTOM SECTION: Navigation & Submission Actions ---
        HBox bottomBox = new HBox(20);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        Button earlySubmitBtn = new Button("Submit Early  🏁");
        earlySubmitBtn.getStyleClass().addAll("button", "button-outline");
        earlySubmitBtn.setOnAction(e -> handleEarlySubmitConfirmation());

        Region botSpacer = new Region();
        HBox.setHgrow(botSpacer, Priority.ALWAYS);

        nextButton = new Button("Next Question  →");
        nextButton.getStyleClass().addAll("button", "button-primary");
        nextButton.setPrefHeight(46);
        nextButton.setPrefWidth(220);
        nextButton.setDisable(true); // Disabled until user selects/types answer
        nextButton.setOnAction(e -> handleNextOrSubmit());

        bottomBox.getChildren().addAll(earlySubmitBtn, botSpacer, nextButton);
        root.setBottom(bottomBox);
    }

    private void setupTimer() {
        timerService.setOnTick(remainingSeconds -> {
            quiz.setTimeRemainingSeconds(remainingSeconds);
            timerLabel.setText(timerService.getFormattedTime());

            double progress = timerService.getProgress();
            progressBar.setProgress(progress);

            // Dynamic Urgency Feedback as time elapses
            timerCapsule.getStyleClass().removeAll("timer-normal", "timer-warning", "timer-critical");
            progressBar.getStyleClass().removeAll("progress-normal", "progress-warning", "progress-danger");

            if (progress > 0.5) {
                timerCapsule.getStyleClass().add("timer-normal");
                progressBar.getStyleClass().add("progress-normal");
            } else if (progress > 0.2) {
                timerCapsule.getStyleClass().add("timer-warning");
                progressBar.getStyleClass().add("progress-warning");
            } else {
                timerCapsule.getStyleClass().add("timer-critical");
                progressBar.getStyleClass().add("progress-danger");
            }
        });

        // Auto-Submit on Timeout
        timerService.setOnTimeout(() -> {
            captureCurrentAnswer();
            quiz.submit();
            showTimeoutAlertAndFinish();
        });

        timerService.start();
    }

    private void renderCurrentQuestion() {
        Question current = quiz.getCurrentQuestion();
        if (current == null) {
            finishQuiz();
            return;
        }

        int qNumber = quiz.getCurrentIndex() + 1;
        progressTextLabel.setText("Question " + qNumber + " of " + quiz.getTotalQuestions());
        questionBadgeLabel.setText("QUESTION " + qNumber + " OF " + quiz.getTotalQuestions());
        questionTextLabel.setText(current.getText());

        answerContainer.getChildren().clear();
        nextButton.setDisable(true); // Must provide answer to advance

        if (current instanceof MultipleChoiceQuestion mcq) {
            renderMultipleChoice(mcq);
        } else if (current instanceof ShortAnswerQuestion saq) {
            renderShortAnswer(saq);
        }

        // Update button text on last question
        if (!quiz.hasNext()) {
            nextButton.setText("Finish & Submit Quiz  ✓");
            nextButton.getStyleClass().remove("button-primary");
            nextButton.getStyleClass().add("button-success");
        } else {
            nextButton.setText("Next Question  →");
            nextButton.getStyleClass().remove("button-success");
            nextButton.getStyleClass().add("button-primary");
        }
    }

    private void renderMultipleChoice(MultipleChoiceQuestion mcq) {
        mcqToggleGroup = new ToggleGroup();
        char optionLetter = 'A';

        for (Option opt : mcq.getOptions()) {
            String labelText = optionLetter + ".   " + opt.getOptionText();
            RadioButton rb = new RadioButton(labelText);
            rb.setToggleGroup(mcqToggleGroup);
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setUserData(opt.getOptionText());

            if (mcq.getUserAnswer() != null && mcq.getUserAnswer().equals(opt.getOptionText())) {
                rb.setSelected(true);
                nextButton.setDisable(false);
            }

            rb.setOnAction(e -> {
                mcq.setUserAnswer(opt.getOptionText());
                nextButton.setDisable(false);
            });

            answerContainer.getChildren().add(rb);
            optionLetter++;
        }

        mcqToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                RadioButton sel = (RadioButton) newVal;
                String actualAnswer = (String) sel.getUserData();
                mcq.setUserAnswer(actualAnswer != null ? actualAnswer : sel.getText());
                nextButton.setDisable(false);
            }
        });
    }

    private void renderShortAnswer(ShortAnswerQuestion saq) {
        VBox saBox = new VBox(10);
        saBox.setPadding(new Insets(14));
        saBox.getStyleClass().add("card-subtle");

        Label hint = new Label("💡 Type your response in the box below (case-insensitive keyword matching):");
        hint.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 13px; -fx-font-weight: 600;");

        shortAnswerField = new TextField();
        shortAnswerField.setPromptText("Type your answer here and press Enter or click Next...");
        shortAnswerField.setPrefHeight(48);
        shortAnswerField.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-control-inner-background: #0b1220; -fx-text-fill: #ffffff;");

        if (saq.getUserAnswer() != null && !saq.getUserAnswer().isEmpty()) {
            shortAnswerField.setText(saq.getUserAnswer());
            nextButton.setDisable(false);
        }

        shortAnswerField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasText = newVal != null && !newVal.trim().isEmpty();
            saq.setUserAnswer(newVal != null ? newVal.trim() : "");
            nextButton.setDisable(!hasText);
        });

        shortAnswerField.setOnAction(e -> {
            if (!nextButton.isDisable()) {
                handleNextOrSubmit();
            }
        });

        saBox.getChildren().addAll(hint, shortAnswerField);
        answerContainer.getChildren().add(saBox);
    }

    private void captureCurrentAnswer() {
        Question current = quiz.getCurrentQuestion();
        if (current == null) return;

        if (current instanceof MultipleChoiceQuestion) {
            if (mcqToggleGroup != null && mcqToggleGroup.getSelectedToggle() != null) {
                RadioButton sel = (RadioButton) mcqToggleGroup.getSelectedToggle();
                String actual = (String) sel.getUserData();
                current.setUserAnswer(actual != null ? actual : sel.getText());
            }
        } else if (current instanceof ShortAnswerQuestion) {
            if (shortAnswerField != null) {
                current.setUserAnswer(shortAnswerField.getText().trim());
            }
        }
    }

    private void handleNextOrSubmit() {
        captureCurrentAnswer();

        if (quiz.hasNext()) {
            quiz.nextQuestion();
            renderCurrentQuestion();
        } else {
            timerService.stop();
            quiz.submit();
            finishQuiz();
        }
    }

    private void handleEarlySubmitConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Submit Exam Early?");
        alert.setHeaderText("Are you sure you want to finish and submit now?");
        alert.setContentText("Any unanswered questions will be marked as incorrect.\nNote: The countdown timer continues running.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                captureCurrentAnswer();
                timerService.stop();
                quiz.submit();
                finishQuiz();
            }
        });
    }

    private void showTimeoutAlertAndFinish() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Time Expired!");
        alert.setHeaderText("Time's up!");
        alert.setContentText("Your quiz time has expired. ChronoQuiz has automatically submitted your answers.");
        alert.showAndWait();

        finishQuiz();
    }

    private void finishQuiz() {
        timerService.stop();
        NavigationManager.getInstance().showResultsScreen(quiz, null);
    }
}
