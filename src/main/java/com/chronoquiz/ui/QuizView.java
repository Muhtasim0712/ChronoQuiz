package com.chronoquiz.ui;

import com.chronoquiz.model.*;
import com.chronoquiz.service.QuizTimerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/**
 * Live Quiz Screen: Displays questions, manages user input, renders dynamic controls
 * (radio buttons vs text fields), and integrates the background ScheduledExecutorService countdown timer.
 */
public class QuizView {
    private final BorderPane root = new BorderPane();
    private final Quiz quiz;
    private final QuizTimerService timerService;

    private Label timerLabel;
    private ProgressBar progressBar;
    private Label progressTextLabel;
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
        root.setPadding(new Insets(24, 40, 30, 40));

        // --- TOP SECTION: Timer & Progress Bar ---
        VBox topBox = new VBox(12);

        HBox metaRow = new HBox(15);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label(quiz.getCategoryName());
        catBadge.getStyleClass().addAll("badge", "badge-source");

        Label diffBadge = new Label(quiz.getDifficulty().getDisplayName());
        String diffClass = switch (quiz.getDifficulty()) {
            case EASY -> "badge-easy";
            case HARD -> "badge-hard";
            default -> "badge-medium";
        };
        diffBadge.getStyleClass().addAll("badge", diffClass);

        Label lockBadge = new Label("🔒 Timer Locked");
        lockBadge.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-padding: 2px 8px; -fx-background-color: #1e293b; -fx-background-radius: 10px;");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        timerLabel = new Label(timerService.getFormattedTime());
        timerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #10b981;");

        metaRow.getChildren().addAll(catBadge, diffBadge, lockBadge, spacer1, timerLabel);

        // Progress row
        HBox progressRow = new HBox(12);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        progressTextLabel = new Label("Question 1 of " + quiz.getTotalQuestions());
        progressTextLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");

        progressBar = new ProgressBar(1.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.getStyleClass().addAll("progress-bar", "progress-normal");
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        progressRow.getChildren().addAll(progressTextLabel, progressBar);

        topBox.getChildren().addAll(metaRow, progressRow);
        root.setTop(topBox);

        // --- CENTER SECTION: Question Card ---
        VBox centerCard = new VBox(24);
        centerCard.getStyleClass().add("card");
        centerCard.setAlignment(Pos.TOP_LEFT);
        centerCard.setMaxWidth(860);

        questionTextLabel = new Label();
        questionTextLabel.setWrapText(true);
        questionTextLabel.getStyleClass().add("title-medium");
        questionTextLabel.setMinHeight(60);

        answerContainer = new VBox(14);
        answerContainer.setAlignment(Pos.CENTER_LEFT);

        centerCard.getChildren().addAll(questionTextLabel, answerContainer);

        StackPane centerWrapper = new StackPane(centerCard);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setPadding(new Insets(20, 0, 20, 0));
        root.setCenter(centerWrapper);

        // --- BOTTOM SECTION: Navigation Buttons ---
        HBox bottomBox = new HBox(20);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);

        Button earlySubmitBtn = new Button("Submit Early");
        earlySubmitBtn.getStyleClass().addAll("button", "button-outline");
        earlySubmitBtn.setOnAction(e -> handleEarlySubmitConfirmation());

        Region botSpacer = new Region();
        HBox.setHgrow(botSpacer, Priority.ALWAYS);

        nextButton = new Button("Next Question");
        nextButton.getStyleClass().addAll("button", "button-primary");
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

            // Dynamic color feedback as time decreases
            progressBar.getStyleClass().removeAll("progress-normal", "progress-warning", "progress-danger");
            if (progress > 0.5) {
                progressBar.getStyleClass().add("progress-normal");
                timerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #10b981;");
            } else if (progress > 0.2) {
                progressBar.getStyleClass().add("progress-warning");
                timerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #f59e0b;");
            } else {
                progressBar.getStyleClass().add("progress-danger");
                timerLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #ef4444;");
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
        questionTextLabel.setText(qNumber + ".  " + current.getText());

        answerContainer.getChildren().clear();
        nextButton.setDisable(true); // Must provide answer to advance

        if (current instanceof MultipleChoiceQuestion mcq) {
            renderMultipleChoice(mcq);
        } else if (current instanceof ShortAnswerQuestion saq) {
            renderShortAnswer(saq);
        }

        // Update button text on last question
        if (!quiz.hasNext()) {
            nextButton.setText("Submit Quiz");
            nextButton.getStyleClass().remove("button-primary");
            nextButton.getStyleClass().add("button-success");
        } else {
            nextButton.setText("Next Question");
            nextButton.getStyleClass().remove("button-success");
            nextButton.getStyleClass().add("button-primary");
        }
    }

    private void renderMultipleChoice(MultipleChoiceQuestion mcq) {
        mcqToggleGroup = new ToggleGroup();

        for (Option opt : mcq.getOptions()) {
            RadioButton rb = new RadioButton(opt.getOptionText());
            rb.setToggleGroup(mcqToggleGroup);
            rb.setMaxWidth(Double.MAX_VALUE);

            if (mcq.getUserAnswer() != null && mcq.getUserAnswer().equals(opt.getOptionText())) {
                rb.setSelected(true);
                nextButton.setDisable(false);
            }

            rb.setOnAction(e -> {
                mcq.setUserAnswer(opt.getOptionText());
                nextButton.setDisable(false);
            });

            answerContainer.getChildren().add(rb);
        }

        mcqToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                RadioButton sel = (RadioButton) newVal;
                mcq.setUserAnswer(sel.getText());
                nextButton.setDisable(false);
            }
        });
    }

    private void renderShortAnswer(ShortAnswerQuestion saq) {
        Label hint = new Label("Type your answer below (case-insensitive keyword matching):");
        hint.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        shortAnswerField = new TextField();
        shortAnswerField.setPromptText("Enter your answer here...");
        shortAnswerField.setPrefHeight(45);

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

        answerContainer.getChildren().addAll(hint, shortAnswerField);
    }

    private void captureCurrentAnswer() {
        Question current = quiz.getCurrentQuestion();
        if (current == null) return;

        if (current instanceof MultipleChoiceQuestion) {
            if (mcqToggleGroup != null && mcqToggleGroup.getSelectedToggle() != null) {
                RadioButton sel = (RadioButton) mcqToggleGroup.getSelectedToggle();
                current.setUserAnswer(sel.getText());
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
