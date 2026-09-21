package com.chronoquiz.ui;

import com.chronoquiz.api.ApiService;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.db.SettingsDAO;
import com.chronoquiz.model.Difficulty;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.Quiz;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

/**
 * Participant Examination Portal (Start Screen):
 * Streamlined portal where participants enter their ID/Name to participate in the examination.
 * Exam rules (time limit, questions, category, difficulty) are centrally configured by the Administrator.
 * Admin portal is accessed via password authentication.
 */
public class StartView {
    private final BorderPane root = new BorderPane();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final ApiService apiService = new ApiService();

    private TextField participantIdField;
    private Button startExamBtn;
    private ProgressIndicator loadingIndicator;
    private Label errorNoticeLabel;

    public StartView() {
        buildUI();
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(24, 40, 30, 40));

        // --- Top Bar: Branding and Admin Login ---
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox brandBox = new VBox(2);
        Label title = new Label("ChronoQuiz");
        title.getStyleClass().add("title-large");
        Label subtitle = new Label("Secure Timed Desktop Examination Portal");
        subtitle.getStyleClass().add("subtitle");
        brandBox.getChildren().addAll(title, subtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button adminPortalBtn = new Button("🔒 Administrator Portal");
        adminPortalBtn.getStyleClass().addAll("button", "button-outline");
        adminPortalBtn.setStyle("-fx-border-color: #6366f1; -fx-text-fill: #a5b4fc; -fx-font-weight: bold;");
        adminPortalBtn.setOnAction(e -> handleAdminLogin());

        topBox.getChildren().addAll(brandBox, topSpacer, adminPortalBtn);
        root.setTop(topBox);

        // --- Center: Participant Sign-In & Exam Info Card ---
        VBox centerCard = new VBox(20);
        centerCard.getStyleClass().add("card-accent");
        centerCard.setMaxWidth(680);
        centerCard.setAlignment(Pos.TOP_LEFT);

        Label formTitle = new Label("Participant Examination Entry");
        formTitle.getStyleClass().add("title-medium");

        Label formDesc = new Label("Please enter your Student / Participant ID or Name to begin. All exam parameters and timer duration are fixed by the administrator.");
        formDesc.setWrapText(true);
        formDesc.getStyleClass().add("subtitle");

        // Active Exam Summary Badge Card
        VBox examSummaryCard = buildExamSummaryCard();

        // Participant ID Input Form
        VBox inputSection = new VBox(10);
        Label idLabel = new Label("Participant ID or Full Name:");
        idLabel.getStyleClass().add("label-field");

        participantIdField = new TextField();
        participantIdField.setPromptText("e.g. STU-2024-001 or Jane Doe");
        participantIdField.setPrefHeight(45);
        participantIdField.setStyle("-fx-font-size: 15px; -fx-control-inner-background: #0b1329; -fx-text-fill: #f8fafc;");

        errorNoticeLabel = new Label();
        errorNoticeLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
        errorNoticeLabel.setVisible(false);

        participantIdField.setOnAction(e -> handleStartExamination());

        inputSection.getChildren().addAll(idLabel, participantIdField, errorNoticeLabel);

        // Actions: Start Examination Button
        HBox actionBox = new HBox(16);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        startExamBtn = new Button("🚀 Start Timed Examination");
        startExamBtn.getStyleClass().addAll("button", "button-primary");
        startExamBtn.setPrefHeight(48);
        startExamBtn.setPrefWidth(260);
        startExamBtn.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");
        startExamBtn.setOnAction(e -> handleStartExamination());

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(30, 30);
        loadingIndicator.setVisible(false);

        actionBox.getChildren().addAll(startExamBtn, loadingIndicator);

        centerCard.getChildren().addAll(formTitle, formDesc, examSummaryCard, inputSection, actionBox);

        StackPane centerWrapper = new StackPane(centerCard);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setPadding(new Insets(20, 0, 10, 0));
        root.setCenter(centerWrapper);

        // --- Bottom Bar ---
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(16, 0, 0, 0));

        Label footerLabel = new Label("ChronoQuiz Examination Engine • SQLite High-Performance Persistence • Anti-Tamper Timer");
        footerLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
        bottomBox.getChildren().add(footerLabel);

        root.setBottom(bottomBox);
    }

    private VBox buildExamSummaryCard() {
        VBox box = new VBox(12);
        box.getStyleClass().add("card-subtle");
        box.setPadding(new Insets(16, 18, 16, 18));

        Label header = new Label("ACTIVE EXAMINATION SPECIFICATION");
        header.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #38bdf8; -fx-letter-spacing: 1px;");

        // Read active parameters from database settings
        int timeSec = settingsDAO.getQuizTimeLimitSeconds();
        int mins = timeSec / 60;
        int remSec = timeSec % 60;
        String formattedTimer = String.format("%02d:%02d (%d seconds)", mins, remSec, timeSec);

        int questionCount = settingsDAO.getQuizQuestionCount();
        String categoryName = settingsDAO.getQuizCategoryName();
        Difficulty difficulty = settingsDAO.getQuizDifficulty();
        String source = settingsDAO.getQuizSource();

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(8);

        grid.add(createSpecItem("Subject / Category:", categoryName), 0, 0);
        grid.add(createSpecItem("Questions:", questionCount + " items"), 1, 0);

        grid.add(createSpecItem("Difficulty:", difficulty.getDisplayName()), 0, 1);
        grid.add(createSpecItem("Time Limit:", formattedTimer), 1, 1);

        grid.add(createSpecItem("Question Source:", source), 0, 2);

        Label timerLockNote = new Label("⏱ Timer Lockdown: Timer will run non-stop once started and cannot be paused or changed.");
        timerLockNote.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px; -fx-font-weight: 600;");

        box.getChildren().addAll(header, new Separator(), grid, new Separator(), timerLockNote);
        return box;
    }

    private HBox createSpecItem(String title, String value) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);

        Label t = new Label(title);
        t.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: bold;");
        t.setMinWidth(125);

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px; -fx-font-weight: 600;");

        row.getChildren().addAll(t, v);
        return row;
    }

    private void handleStartExamination() {
        String participantId = participantIdField.getText().trim();
        if (participantId.isEmpty()) {
            errorNoticeLabel.setText("⚠ Please enter your Participant ID or Name to proceed.");
            errorNoticeLabel.setVisible(true);
            participantIdField.requestFocus();
            return;
        }

        errorNoticeLabel.setVisible(false);

        // Read active settings
        int timeLimitSeconds = settingsDAO.getQuizTimeLimitSeconds();
        int questionCount = settingsDAO.getQuizQuestionCount();
        int catId = settingsDAO.getQuizCategoryId();
        String catName = settingsDAO.getQuizCategoryName();
        Difficulty difficulty = settingsDAO.getQuizDifficulty();
        String source = settingsDAO.getQuizSource();

        boolean isOnline = "Online Trivia API (OpenTDB)".equalsIgnoreCase(source);

        if (isOnline) {
            fetchOnlineAndStart(participantId, catId, catName, difficulty, questionCount, timeLimitSeconds);
        } else {
            startLocalExamination(participantId, catId, catName, difficulty, questionCount, timeLimitSeconds);
        }
    }

    private void startLocalExamination(String participantId, int catId, String catName, Difficulty difficulty, int amount, int timeLimit) {
        Integer queryCatId = catId > 0 ? catId : null;
        List<Question> questions = questionDAO.getQuestions(queryCatId, difficulty, amount);

        if (questions.isEmpty()) {
            // Fallback to any available questions in local database
            questions = questionDAO.getQuestions(null, Difficulty.ANY, amount);
        }

        if (questions.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Question Bank Empty",
                    "No questions are currently available in the database.\nPlease contact the administrator to populate questions.");
            return;
        }

        Quiz quiz = new Quiz(participantId, catName, catId, difficulty, questions, timeLimit);
        NavigationManager.getInstance().showQuizScreen(quiz);
    }

    private void fetchOnlineAndStart(String participantId, int catId, String catName, Difficulty difficulty, int amount, int timeLimit) {
        startExamBtn.setDisable(true);
        loadingIndicator.setVisible(true);

        Integer queryCatId = catId > 0 ? catId : null;

        apiService.fetchQuestionsAsync(amount, queryCatId, difficulty)
                .thenAccept(questions -> Platform.runLater(() -> {
                    startExamBtn.setDisable(false);
                    loadingIndicator.setVisible(false);

                    Quiz quiz = new Quiz(participantId, catName, catId, difficulty, questions, timeLimit);
                    NavigationManager.getInstance().showQuizScreen(quiz);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        startExamBtn.setDisable(false);
                        loadingIndicator.setVisible(false);

                        showAlert(Alert.AlertType.WARNING, "Online API Offline / Fallback",
                                "Could not fetch questions from Trivia API: " + ex.getMessage() +
                                        "\n\nFalling back to local SQLite question bank.");

                        startLocalExamination(participantId, catId, catName, difficulty, amount, timeLimit);
                    });
                    return null;
                });
    }

    private void handleAdminLogin() {
        Dialog<String> loginDialog = new Dialog<>();
        loginDialog.setTitle("Administrator Authentication");
        loginDialog.setHeaderText("Enter Admin Password to access examination controls.");

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        Label prompt = new Label("Password:");
        prompt.getStyleClass().add("label-field");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter administrator password");
        passwordField.setPrefWidth(280);

        content.getChildren().addAll(prompt, passwordField);
        loginDialog.getDialogPane().setContent(content);

        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        loginDialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        Platform.runLater(passwordField::requestFocus);

        loginDialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> result = loginDialog.showAndWait();
        result.ifPresent(enteredPassword -> {
            String actualPassword = settingsDAO.getAdminPassword();
            if (actualPassword.equals(enteredPassword)) {
                NavigationManager.getInstance().showAdminDashboard();
            } else {
                showAlert(Alert.AlertType.ERROR, "Authentication Failed", "Incorrect administrator password.\nAccess denied.");
            }
        });
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
