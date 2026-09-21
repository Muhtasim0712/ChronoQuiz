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
 * Answer Review Screen: Displays detailed side-by-side comparison of user answers
 * against correct answers with color-coded feedback.
 */
public class ReviewView {
    private final BorderPane root = new BorderPane();
    private final Quiz quiz;
    private final Attempt attempt;
    private final AttemptDAO attemptDAO = new AttemptDAO();

    public ReviewView(Quiz quiz, Attempt attempt) {
        this.quiz = quiz;
        this.attempt = attempt;

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

        Button backBtn = new Button("← Back to Results");
        backBtn.getStyleClass().addAll("button", "button-outline");
        backBtn.setOnAction(e -> NavigationManager.getInstance().showResultsScreen(quiz, attempt));

        VBox titleBox = new VBox(2);
        Label title = new Label("Answer Key & Detailed Review");
        title.getStyleClass().add("title-medium");

        Label subtitle = new Label("Review your selected/typed answers alongside the verified correct solutions.");
        subtitle.getStyleClass().add("subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button menuBtn = new Button("Main Menu");
        menuBtn.getStyleClass().addAll("button", "button-primary");
        menuBtn.setOnAction(e -> NavigationManager.getInstance().showStartScreen());

        topBox.getChildren().addAll(backBtn, titleBox, topSpacer, menuBtn);
        root.setTop(topBox);

        // Center: Scrollable list of question review cards
        VBox cardsList = new VBox(16);
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
        VBox card = new VBox(12);
        card.setMaxWidth(860);

        if (isCorrect) {
            card.getStyleClass().add("review-card-correct");
        } else {
            card.getStyleClass().add("review-card-incorrect");
        }

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label numLbl = new Label("Question " + number);
        numLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #f8fafc;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(isCorrect ? "✓ CORRECT" : "✗ INCORRECT");
        statusBadge.getStyleClass().add("badge");
        if (isCorrect) {
            statusBadge.setStyle("-fx-background-color: #10b981; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        } else {
            statusBadge.setStyle("-fx-background-color: #ef4444; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
        }

        headerRow.getChildren().addAll(numLbl, spacer, statusBadge);

        Label qText = new Label(questionText);
        qText.setWrapText(true);
        qText.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #ffffff;");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 4, 0));

        Label userPrompt = new Label("Your Answer:");
        userPrompt.setStyle("-fx-font-weight: bold; -fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        Label userVal = new Label(userAnswer);
        userVal.setStyle("-fx-font-weight: 600; -fx-text-fill: " + (isCorrect ? "#34d399" : "#f87171") + "; -fx-font-size: 13px;");

        Label correctPrompt = new Label("Correct Answer:");
        correctPrompt.setStyle("-fx-font-weight: bold; -fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        Label correctVal = new Label(correctAnswer);
        correctVal.setStyle("-fx-font-weight: 600; -fx-text-fill: #34d399; -fx-font-size: 13px;");

        grid.add(userPrompt, 0, 0);
        grid.add(userVal, 1, 0);
        grid.add(correctPrompt, 0, 1);
        grid.add(correctVal, 1, 1);

        card.getChildren().addAll(headerRow, qText, grid);
        return card;
    }
}
