package com.chronoquiz.ui;

import com.chronoquiz.db.AttemptDAO;
import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.AttemptAnswer;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.Quiz;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Answer Review Screen:
 * Displays detailed side-by-side comparison of user answers against verified solutions
 * with high-contrast color-coded student feedback cards.
 */
public class ReviewView {
    private final BorderPane root = new BorderPane();
    private final Quiz quiz;
    private final Attempt attempt;
    private final boolean fromHistory;
    private final AttemptDAO attemptDAO = new AttemptDAO();

    public ReviewView(Quiz quiz, Attempt attempt) {
        this(quiz, attempt, quiz == null);
    }

    public ReviewView(Quiz quiz, Attempt attempt, boolean fromHistory) {
        this.quiz = quiz;
        this.attempt = attempt;
        this.fromHistory = fromHistory;

        buildUI();
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(24, 40, 30, 40));

        // Header
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button(fromHistory ? "← Back to Attempt History" : "← Back to Results");
        backBtn.getStyleClass().addAll("button", "button-outline");
        if (fromHistory) {
            backBtn.setOnAction(e -> NavigationManager.getInstance().showHistoryScreen());
        } else {
            backBtn.setOnAction(e -> NavigationManager.getInstance().showResultsScreen(quiz, attempt));
        }

        VBox titleBox = new VBox(3);
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Answer Key & Detailed Review");
        title.getStyleClass().add("title-medium");

        String studentName = quiz != null ? quiz.getUserName() : (attempt != null ? attempt.getUserName() : "Student");
        Label studentBadge = new Label("👤 " + studentName);
        studentBadge.getStyleClass().addAll("badge", "badge-source");

        titleRow.getChildren().addAll(title, studentBadge);

        Label subtitle = new Label("Compare your responses directly with the verified solutions to learn and improve.");
        subtitle.getStyleClass().add("subtitle");
        titleBox.getChildren().addAll(titleRow, subtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button exitBtn = new Button(fromHistory ? "Admin Dashboard 🔒" : "Main Menu 🏠");
        exitBtn.getStyleClass().addAll("button", "button-primary");
        if (fromHistory) {
            exitBtn.setOnAction(e -> NavigationManager.getInstance().showAdminDashboard());
        } else {
            exitBtn.setOnAction(e -> NavigationManager.getInstance().showStartScreen());
        }

        topBox.getChildren().addAll(backBtn, titleBox, topSpacer, exitBtn);
        root.setTop(topBox);

        // Center: Scrollable list of question review cards
        VBox cardsList = new VBox(18);
        cardsList.setPadding(new Insets(20, 10, 20, 0));
        cardsList.setAlignment(Pos.TOP_CENTER);

        if (quiz != null) {
            renderFromQuiz(cardsList);
        } else if (attempt != null) {
            renderFromAttempt(cardsList);
        }

        ScrollPane scrollPane = new ScrollPane(cardsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setPadding(new Insets(0, 10, 0, 0));

        root.setCenter(scrollPane);
    }

    private void renderFromQuiz(VBox container) {
        List<Question> questions = quiz.getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            boolean isCorrect = q.isCorrect();
            String userAns = (q.getUserAnswer() != null && !q.getUserAnswer().isEmpty())
                    ? q.getUserAnswer() : "[No Answer Provided]";
            String correctAns = q.getCorrectAnswerDisplay();

            VBox card = createReviewCard(i + 1, q.getText(), userAns, correctAns, isCorrect);
            container.getChildren().add(card);
        }
    }

    private void renderFromAttempt(VBox container) {
        List<AttemptAnswer> answers = attemptDAO.getAnswersForAttempt(attempt.getId());
        if (answers.isEmpty()) {
            answers = attempt.getAnswers();
        }

        for (int i = 0; i < answers.size(); i++) {
            AttemptAnswer ans = answers.get(i);
            boolean isCorrect = ans.isCorrect();
            String userAns = (ans.getUserAnswer() != null && !ans.getUserAnswer().isEmpty())
                    ? ans.getUserAnswer() : "[No Answer Provided]";
            String correctAns = ans.getCorrectAnswer();

            VBox card = createReviewCard(i + 1, ans.getQuestionText(), userAns, correctAns, isCorrect);
            container.getChildren().add(card);
        }
    }

    private VBox createReviewCard(int number, String questionText, String userAnswer, String correctAnswer, boolean isCorrect) {
        VBox card = new VBox(14);
        card.setMaxWidth(880);

        if (isCorrect) {
            card.getStyleClass().add("review-card-correct");
        } else {
            card.getStyleClass().add("review-card-incorrect");
        }

        // Header Row: Question number badge & Correctness Pill
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label numBadge = new Label("QUESTION " + number);
        numBadge.setStyle("-fx-font-weight: 800; -fx-font-size: 12px; -fx-text-fill: #38bdf8; -fx-padding: 3px 10px; -fx-background-color: rgba(6, 182, 212, 0.15); -fx-background-radius: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(isCorrect ? "✓ CORRECT" : "✗ INCORRECT");
        statusBadge.getStyleClass().add("badge");
        if (isCorrect) {
            statusBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.25); -fx-text-fill: #34d399; -fx-border-color: #10b981; -fx-border-radius: 20px; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 4px 12px;");
        } else {
            statusBadge.setStyle("-fx-background-color: rgba(244, 63, 94, 0.25); -fx-text-fill: #fb7185; -fx-border-color: #f43f5e; -fx-border-radius: 20px; -fx-font-weight: 800; -fx-font-size: 12px; -fx-padding: 4px 12px;");
        }

        headerRow.getChildren().addAll(numBadge, spacer, statusBadge);

        // Question Prompt
        Label qText = new Label(questionText);
        qText.setWrapText(true);
        qText.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #ffffff; -fx-line-spacing: 3px;");

        // Two Distinct Answer Blocks
        HBox answersRow = new HBox(16);
        answersRow.setAlignment(Pos.CENTER_LEFT);

        // User Answer Box
        VBox userBox = new VBox(6);
        userBox.setPadding(new Insets(12, 16, 12, 16));
        userBox.setStyle(isCorrect
                ? "-fx-background-color: rgba(16, 185, 129, 0.12); -fx-background-radius: 12px; -fx-border-color: #10b981; -fx-border-radius: 12px; -fx-border-width: 1px;"
                : "-fx-background-color: rgba(244, 63, 94, 0.12); -fx-background-radius: 12px; -fx-border-color: #f43f5e; -fx-border-radius: 12px; -fx-border-width: 1px;");
        HBox.setHgrow(userBox, Priority.ALWAYS);

        Label userLabel = new Label(isCorrect ? "✓ Your Answer (Correct)" : "✗ Your Answer (Incorrect)");
        userLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + (isCorrect ? "#34d399" : "#fb7185") + ";");

        Label userValue = new Label(userAnswer);
        userValue.setWrapText(true);
        userValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #ffffff;");
        userBox.getChildren().addAll(userLabel, userValue);

        // Correct Answer Box
        VBox correctBox = new VBox(6);
        correctBox.setPadding(new Insets(12, 16, 12, 16));
        correctBox.setStyle("-fx-background-color: rgba(6, 182, 212, 0.12); -fx-background-radius: 12px; -fx-border-color: #06b6d4; -fx-border-radius: 12px; -fx-border-width: 1px;");
        HBox.setHgrow(correctBox, Priority.ALWAYS);

        Label correctLabel = new Label("🎯 Verified Correct Solution");
        correctLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");

        Label correctValue = new Label(correctAnswer);
        correctValue.setWrapText(true);
        correctValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #38bdf8;");
        correctBox.getChildren().addAll(correctLabel, correctValue);

        answersRow.getChildren().addAll(userBox, correctBox);

        card.getChildren().addAll(headerRow, qText, answersRow);
        return card;
    }
}
