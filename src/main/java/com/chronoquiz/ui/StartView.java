package com.chronoquiz.ui;

import com.chronoquiz.api.ApiService;
import com.chronoquiz.db.CategoryDAO;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.db.SettingsDAO;
import com.chronoquiz.model.Category;
import com.chronoquiz.model.Difficulty;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.Quiz;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Optional;

/**
 * Modern Student-Friendly Start Screen for ChronoQuiz:
 * Features prominent branding, inspiring tagline, student ID/name input,
 * active category and difficulty selection, clear question & time information badges,
 * and a large vibrant "Start Quiz" button.
 */
public class StartView {
    private final BorderPane root = new BorderPane();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final ApiService apiService = new ApiService();

    private TextField nameField;
    private ComboBox<CategoryItem> categoryCombo;
    private Label categoryDescLabel;
    private ComboBox<Difficulty> difficultyCombo;
    private Label questionCountLabel;
    private Label timeLimitLabel;
    private Label timerPolicyLabel;
    private Button startQuizBtn;
    private ProgressIndicator loadingIndicator;
    private Label errorNoticeLabel;

    public StartView() {
        buildUI();
        loadCategoriesAndDefaults();
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(24, 44, 30, 44));

        // --- Top Header: Logo, Tagline & Admin Access ---
        HBox topBox = new HBox(20);
        topBox.setAlignment(Pos.CENTER_LEFT);

        // Logo with University/Academic Badge
        HBox logoBadge = new HBox(12);
        logoBadge.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("🎓");
        logoIcon.setStyle("-fx-font-size: 28px; -fx-padding: 8px 12px; -fx-background-color: linear-gradient(135deg, rgba(99, 102, 241, 0.3), rgba(6, 182, 212, 0.3)); -fx-background-radius: 14px; -fx-border-color: rgba(99, 102, 241, 0.5); -fx-border-radius: 14px;");

        VBox titleBox = new VBox(2);
        Label title = new Label("ChronoQuiz");
        title.getStyleClass().add("title-hero");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: linear-gradient(to right, #ffffff, #c7d2fe, #38bdf8);");

        Label tagline = new Label("Challenge Yourself. Learn. Improve.");
        tagline.getStyleClass().add("tagline");

        titleBox.getChildren().addAll(title, tagline);
        logoBadge.getChildren().addAll(logoIcon, titleBox);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button adminPortalBtn = new Button("🔒 Administrator Portal");
        adminPortalBtn.getStyleClass().addAll("button", "button-outline");
        adminPortalBtn.setStyle("-fx-border-color: #6366f1; -fx-text-fill: #c7d2fe; -fx-font-weight: bold; -fx-background-color: rgba(99, 102, 241, 0.1);");
        adminPortalBtn.setOnAction(e -> handleAdminLogin());

        topBox.getChildren().addAll(logoBadge, topSpacer, adminPortalBtn);
        root.setTop(topBox);

        // --- Center: Interactive Student Quiz Card ---
        VBox centerCard = new VBox(22);
        centerCard.getStyleClass().add("card-hero");
        centerCard.setMaxWidth(740);
        centerCard.setAlignment(Pos.TOP_LEFT);

        // Card Header Banner
        VBox bannerBox = new VBox(4);
        Label bannerTitle = new Label("Ready to Test Your Knowledge?");
        bannerTitle.getStyleClass().add("title-medium");
        bannerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Label bannerSubtitle = new Label("Enter your student credentials, verify your quiz topic, and begin your timed evaluation.");
        bannerSubtitle.getStyleClass().add("subtitle");
        bannerBox.getChildren().addAll(bannerTitle, bannerSubtitle);

        // Form Fields Container
        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(16);
        formGrid.setAlignment(Pos.CENTER_LEFT);

        // Row 0: Student Name / ID
        Label nameLbl = new Label("👤 Student Name / Participant ID:");
        nameLbl.getStyleClass().add("label-field");

        nameField = new TextField();
        nameField.setPromptText("e.g. STU-2024-001 or Jane Doe");
        nameField.setPrefHeight(46);
        nameField.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-control-inner-background: #0b1220; -fx-text-fill: #ffffff;");
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        formGrid.add(nameLbl, 0, 0);
        formGrid.add(nameField, 1, 0);

        // Row 1: Category Selection
        Label catLbl = new Label("📚 Quiz Category / Subject:");
        catLbl.getStyleClass().add("label-field");

        VBox catContainer = new VBox(4);
        categoryCombo = new ComboBox<>();
        categoryCombo.setPrefHeight(44);
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.setOnAction(e -> updateCategoryDescription());
        GridPane.setHgrow(categoryCombo, Priority.ALWAYS);

        categoryDescLabel = new Label();
        categoryDescLabel.setWrapText(true);
        categoryDescLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #38bdf8; -fx-font-weight: 500;");

        catContainer.getChildren().addAll(categoryCombo, categoryDescLabel);

        formGrid.add(catLbl, 0, 1);
        formGrid.add(catContainer, 1, 1);

        // Row 2: Difficulty Selection
        Label diffLbl = new Label("⚡ Difficulty Level:");
        diffLbl.getStyleClass().add("label-field");

        difficultyCombo = new ComboBox<>(FXCollections.observableArrayList(
                Difficulty.ANY,
                Difficulty.EASY,
                Difficulty.MEDIUM,
                Difficulty.HARD
        ));
        difficultyCombo.setPrefHeight(44);
        difficultyCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(difficultyCombo, Priority.ALWAYS);

        formGrid.add(diffLbl, 0, 2);
        formGrid.add(difficultyCombo, 1, 2);

        // Exam Specifications Display Row (Question Count & Time Limit)
        HBox specCardsRow = new HBox(16);
        specCardsRow.setAlignment(Pos.CENTER);
        specCardsRow.setPadding(new Insets(6, 0, 4, 0));

        // Question count pill card
        VBox qCountCard = createSpecBadge("TOTAL QUESTIONS", "5 Questions", "📝", "#06b6d4");
        questionCountLabel = (Label) qCountCard.getChildren().get(1);

        // Time limit pill card
        VBox timeCard = createSpecBadge("TIME LIMIT", "01:00 (60s)", "⏱", "#34d399");
        timeLimitLabel = (Label) timeCard.getChildren().get(1);

        // Question Source pill card
        VBox srcCard = createSpecBadge("QUESTION SOURCE", settingsDAO.getQuizSource(), "⚡", "#a5b4fc");

        specCardsRow.getChildren().addAll(qCountCard, timeCard, srcCard);
        HBox.setHgrow(qCountCard, Priority.ALWAYS);
        HBox.setHgrow(timeCard, Priority.ALWAYS);
        HBox.setHgrow(srcCard, Priority.ALWAYS);

        // Timer Policy / Notice Banner
        HBox noticeBox = new HBox(10);
        noticeBox.setAlignment(Pos.CENTER_LEFT);
        noticeBox.setPadding(new Insets(10, 14, 10, 14));
        noticeBox.setStyle("-fx-background-color: rgba(99, 102, 241, 0.12); -fx-background-radius: 12px; -fx-border-color: rgba(99, 102, 241, 0.3); -fx-border-radius: 12px; -fx-border-width: 1px;");

        Label lockIcon = new Label("🔒");
        lockIcon.setStyle("-fx-font-size: 16px;");

        timerPolicyLabel = new Label("Anti-Tamper Timer: Once started, the countdown cannot be paused or changed. Answers auto-submit on timeout.");
        timerPolicyLabel.setWrapText(true);
        timerPolicyLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #c7d2fe;");

        noticeBox.getChildren().addAll(lockIcon, timerPolicyLabel);

        // Error message
        errorNoticeLabel = new Label();
        errorNoticeLabel.setStyle("-fx-text-fill: #fb7185; -fx-font-size: 13px; -fx-font-weight: bold;");
        errorNoticeLabel.setVisible(false);

        // Action Section: Large Start Quiz Button
        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        startQuizBtn = new Button("Start Examination  🚀");
        startQuizBtn.getStyleClass().addAll("button", "button-primary");
        startQuizBtn.setPrefHeight(50);
        startQuizBtn.setPrefWidth(280);
        startQuizBtn.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-cursor: hand;");
        startQuizBtn.setOnAction(e -> handleStartQuiz());

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(32, 32);
        loadingIndicator.setVisible(false);

        actionRow.getChildren().addAll(startQuizBtn, loadingIndicator);

        nameField.setOnAction(e -> handleStartQuiz());

        centerCard.getChildren().addAll(bannerBox, formGrid, specCardsRow, noticeBox, errorNoticeLabel, actionRow);

        StackPane centerWrapper = new StackPane(centerCard);
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.setPadding(new Insets(16, 0, 10, 0));

        ScrollPane scrollWrapper = new ScrollPane(centerWrapper);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setStyle("-fx-background-color: transparent;");

        root.setCenter(scrollWrapper);

        // --- Bottom Bar: University Grade Footer ---
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(12, 0, 0, 0));

        Label footer = new Label("ChronoQuiz Academic Testing Engine • SQLite Local Persistence • Anti-Tamper Protection");
        footer.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-weight: 600;");
        bottomBox.getChildren().add(footer);

        root.setBottom(bottomBox);
    }

    private VBox createSpecBadge(String tag, String value, String icon, String hexColor) {
        VBox box = new VBox(3);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card-subtle");
        box.setPadding(new Insets(10, 14, 10, 14));

        HBox top = new HBox(6);
        top.setAlignment(Pos.CENTER_LEFT);
        Label ic = new Label(icon);
        ic.setStyle("-fx-font-size: 12px;");
        Label t = new Label(tag);
        t.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #94a3b8;");
        top.getChildren().addAll(ic, t);

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: " + hexColor + ";");

        box.getChildren().addAll(top, v);
        return box;
    }

    private void loadCategoriesAndDefaults() {
        // Load categories into dropdown
        List<Category> categories = categoryDAO.getAllCategories();
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add(new CategoryItem(0, "All Categories (Mixed)", "Questions drawn from across all available subjects and question banks."));
        for (Category c : categories) {
            categoryCombo.getItems().add(new CategoryItem(c.getId(), c.getName(), c.getDescription()));
        }

        // Pre-select category configured in settings
        int activeCatId = settingsDAO.getQuizCategoryId();
        CategoryItem activeCategory = categoryCombo.getItems().stream()
                .filter(item -> item.id == activeCatId)
                .findFirst()
                .orElse(categoryCombo.getItems().get(0));
        categoryCombo.setValue(activeCategory);
        updateCategoryDescription();

        // Pre-select difficulty
        difficultyCombo.setValue(settingsDAO.getQuizDifficulty());

        // Update Question count & Time limit from settings
        int timeSec = settingsDAO.getQuizTimeLimitSeconds();
        int mins = timeSec / 60;
        int remSec = timeSec % 60;
        timeLimitLabel.setText(String.format("%02d:%02d (%ds)", mins, remSec, timeSec));

        int questionCount = settingsDAO.getQuizQuestionCount();
        questionCountLabel.setText(questionCount + " Questions");
    }

    private void handleStartQuiz() {
        String studentName = nameField.getText().trim();
        if (studentName.isEmpty()) {
            errorNoticeLabel.setText("⚠ Please enter your Student Name or Participant ID before starting.");
            errorNoticeLabel.setVisible(true);
            nameField.requestFocus();
            return;
        }

        errorNoticeLabel.setVisible(false);

        CategoryItem selectedCat = categoryCombo.getValue();
        int catId = selectedCat != null ? selectedCat.id : 0;
        String catName = selectedCat != null ? selectedCat.name : "All Categories";
        Difficulty difficulty = difficultyCombo.getValue() != null ? difficultyCombo.getValue() : Difficulty.ANY;

        int timeLimitSeconds = settingsDAO.getQuizTimeLimitSeconds();
        int questionCount = settingsDAO.getQuizQuestionCount();
        String source = settingsDAO.getQuizSource();

        boolean isOnline = "Online Trivia API (OpenTDB)".equalsIgnoreCase(source);

        if (isOnline) {
            fetchOnlineAndStart(studentName, catId, catName, difficulty, questionCount, timeLimitSeconds);
        } else {
            startLocalQuiz(studentName, catId, catName, difficulty, questionCount, timeLimitSeconds);
        }
    }

    private void startLocalQuiz(String name, int catId, String catName, Difficulty difficulty, int amount, int timeLimit) {
        Integer queryCatId = catId > 0 ? catId : null;
        List<Question> questions = questionDAO.getQuestions(queryCatId, difficulty, amount);

        if (questions.isEmpty()) {
            questions = questionDAO.getQuestions(null, Difficulty.ANY, amount);
        }

        if (questions.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Question Bank Empty",
                    "No questions available in the local database. Please contact your instructor or administrator to populate questions.");
            return;
        }

        Quiz quiz = new Quiz(name, catName, catId, difficulty, questions, timeLimit);
        NavigationManager.getInstance().showQuizScreen(quiz);
    }

    private void fetchOnlineAndStart(String name, int catId, String catName, Difficulty difficulty, int amount, int timeLimit) {
        startQuizBtn.setDisable(true);
        loadingIndicator.setVisible(true);

        Integer queryCatId = catId > 0 ? catId : null;

        apiService.fetchQuestionsAsync(amount, queryCatId, difficulty)
                .thenAccept(questions -> Platform.runLater(() -> {
                    startQuizBtn.setDisable(false);
                    loadingIndicator.setVisible(false);

                    Quiz quiz = new Quiz(name, catName, catId, difficulty, questions, timeLimit);
                    NavigationManager.getInstance().showQuizScreen(quiz);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        startQuizBtn.setDisable(false);
                        loadingIndicator.setVisible(false);

                        showAlert(Alert.AlertType.WARNING, "Trivia API Fallback",
                                "Could not reach online Trivia API: " + ex.getMessage() +
                                        "\n\nSeamlessly launching with local database questions.");

                        startLocalQuiz(name, catId, catName, difficulty, amount, timeLimit);
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

    private void updateCategoryDescription() {
        if (categoryDescLabel == null || categoryCombo == null) return;
        CategoryItem item = categoryCombo.getValue();
        if (item == null) {
            categoryDescLabel.setText("");
        } else if (item.id == 0) {
            categoryDescLabel.setText("🌐 Questions drawn from across all available curriculum subjects.");
        } else if (item.description != null && !item.description.isBlank()) {
            categoryDescLabel.setText("📖 " + item.description);
        } else {
            categoryDescLabel.setText("📖 Subject curriculum examination.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private static class CategoryItem {
        final int id;
        final String name;
        final String description;

        CategoryItem(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description != null ? description : "";
        }

        CategoryItem(int id, String name) {
            this(id, name, "");
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
