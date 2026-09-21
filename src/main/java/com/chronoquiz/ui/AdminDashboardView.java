package com.chronoquiz.ui;

import com.chronoquiz.api.ApiService;
import com.chronoquiz.db.AttemptDAO;
import com.chronoquiz.db.CategoryDAO;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.db.SettingsDAO;
import com.chronoquiz.model.Category;
import com.chronoquiz.model.Difficulty;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Admin Dashboard Screen:
 * Provides password-protected administration for configuring active participant exam parameters
 * (time limit, question count, category, difficulty, source), managing the question bank,
 * reviewing participant attempt history, and updating administrative credentials.
 */
public class AdminDashboardView {
    private final BorderPane root = new BorderPane();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();
    private final ApiService apiService = new ApiService();

    // Active Exam controls
    private TextField timeLimitField;
    private Label timeFormattedLabel;
    private ComboBox<Integer> questionCountCombo;
    private ComboBox<CategoryOption> categoryCombo;
    private ComboBox<Difficulty> difficultyCombo;
    private ComboBox<String> sourceCombo;
    private Label saveStatusLabel;
    private VBox participantPreviewCard;

    // Password controls
    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;
    private Label passwordStatusLabel;

    public AdminDashboardView() {
        buildUI();
        loadCurrentSettings();
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(24, 40, 30, 40));

        // Top Navigation Bar
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Button exitToPortalBtn = new Button("← Participant Portal");
        exitToPortalBtn.getStyleClass().addAll("button", "button-outline");
        exitToPortalBtn.setOnAction(e -> NavigationManager.getInstance().showStartScreen());

        VBox titleBox = new VBox(3);
        Label title = new Label("Administrator Control Panel");
        title.getStyleClass().add("title-medium");
        Label subtitle = new Label("Set participant exam parameters, lock timers, manage question banks, and monitor results.");
        subtitle.getStyleClass().add("subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label adminBadge = new Label("ADMIN ACCESS ACTIVE");
        adminBadge.getStyleClass().addAll("badge", "badge-easy");
        adminBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.2); -fx-text-fill: #34d399; -fx-padding: 6px 14px; -fx-font-weight: bold;");

        Button logoutBtn = new Button("Lock & Logout");
        logoutBtn.getStyleClass().addAll("button", "button-danger");
        logoutBtn.setOnAction(e -> {
            showAlert(Alert.AlertType.INFORMATION, "Admin Session Ended", "You have successfully logged out of the Administrator Control Panel.");
            NavigationManager.getInstance().showStartScreen();
        });

        topBox.getChildren().addAll(exitToPortalBtn, titleBox, topSpacer, adminBadge, logoutBtn);
        root.setTop(topBox);

        // Center: TabPane for cleanly partitioned administrative domains
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setPadding(new Insets(16, 0, 0, 0));

        // Tab 1: Exam & Timer Configuration
        Tab examTab = new Tab("⏱ Exam & Timer Settings", buildExamSettingsTab());

        // Tab 2: Question Bank Manager
        Tab questionTab = new Tab("📚 Question Bank", buildQuestionBankTab());

        // Tab 3: Participant Attempt History
        Tab historyTab = new Tab("📊 Participant History", buildHistoryTab());

        // Tab 4: Security & Credentials
        Tab securityTab = new Tab("🔒 Admin Password", buildSecurityTab());

        tabPane.getTabs().addAll(examTab, questionTab, historyTab, securityTab);
        root.setCenter(tabPane);
    }

    private Region buildExamSettingsTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(20, 10, 20, 10));

        HBox splitBox = new HBox(24);
        splitBox.setAlignment(Pos.TOP_LEFT);

        // Left Panel: Configuration Form
        VBox formCard = new VBox(16);
        formCard.getStyleClass().add("card");
        formCard.setMinWidth(480);
        formCard.setMaxWidth(560);
        HBox.setHgrow(formCard, Priority.ALWAYS);

        Label sectionTitle = new Label("Participant Exam Parameters");
        sectionTitle.getStyleClass().add("title-small");
        Label sectionDesc = new Label("Configure the active exam rules. Participants will take exams with these exact settings.");
        sectionDesc.getStyleClass().add("subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        // 1. Time Limit (Seconds)
        Label timeLbl = new Label("Time Limit (Seconds):");
        timeLbl.getStyleClass().add("label-field");

        VBox timeInputBox = new VBox(6);
        HBox timeRow = new HBox(10);
        timeRow.setAlignment(Pos.CENTER_LEFT);

        timeLimitField = new TextField();
        timeLimitField.setPromptText("e.g. 60");
        timeLimitField.setPrefWidth(100);

        timeFormattedLabel = new Label("= 01:00 (1 min)");
        timeFormattedLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 13px;");

        timeRow.getChildren().addAll(timeLimitField, timeFormattedLabel);

        // Quick Presets
        HBox presetBox = new HBox(8);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        Label presetLbl = new Label("Presets:");
        presetLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        presetBox.getChildren().add(presetLbl);

        int[] presets = {30, 45, 60, 90, 120, 180, 300};
        for (int p : presets) {
            Button pBtn = new Button(p + "s");
            pBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3px 8px; -fx-background-color: #334155; -fx-text-fill: #f8fafc;");
            pBtn.setOnAction(e -> {
                timeLimitField.setText(String.valueOf(p));
                updateTimeFormatted();
                updateParticipantPreview();
            });
            presetBox.getChildren().add(pBtn);
        }

        timeInputBox.getChildren().addAll(timeRow, presetBox);
        grid.add(timeLbl, 0, 0);
        grid.add(timeInputBox, 1, 0);

        timeLimitField.textProperty().addListener((obs, o, n) -> {
            updateTimeFormatted();
            updateParticipantPreview();
        });

        // 2. Question Count
        Label countLbl = new Label("Question Count:");
        countLbl.getStyleClass().add("label-field");

        questionCountCombo = new ComboBox<>(FXCollections.observableArrayList(3, 5, 10, 15, 20, 25));
        questionCountCombo.setMaxWidth(Double.MAX_VALUE);
        questionCountCombo.setOnAction(e -> updateParticipantPreview());
        grid.add(countLbl, 0, 1);
        grid.add(questionCountCombo, 1, 1);

        // 3. Question Source
        Label srcLbl = new Label("Question Source:");
        srcLbl.getStyleClass().add("label-field");

        sourceCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Local Database (SQLite)",
                "Online Trivia API (OpenTDB)"
        ));
        sourceCombo.setMaxWidth(Double.MAX_VALUE);
        sourceCombo.setOnAction(e -> updateParticipantPreview());
        grid.add(srcLbl, 0, 2);
        grid.add(sourceCombo, 1, 2);

        // 4. Subject / Category
        Label catLbl = new Label("Exam Category / Subject:");
        catLbl.getStyleClass().add("label-field");

        categoryCombo = new ComboBox<>();
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.setOnAction(e -> updateParticipantPreview());
        grid.add(catLbl, 0, 3);
        grid.add(categoryCombo, 1, 3);

        // 5. Difficulty Level
        Label diffLbl = new Label("Difficulty Level:");
        diffLbl.getStyleClass().add("label-field");

        difficultyCombo = new ComboBox<>(FXCollections.observableArrayList(
                Difficulty.ANY,
                Difficulty.EASY,
                Difficulty.MEDIUM,
                Difficulty.HARD
        ));
        difficultyCombo.setMaxWidth(Double.MAX_VALUE);
        difficultyCombo.setOnAction(e -> updateParticipantPreview());
        grid.add(diffLbl, 0, 4);
        grid.add(difficultyCombo, 1, 4);

        // Save Button
        Button saveBtn = new Button("💾 Save & Publish Exam Settings");
        saveBtn.getStyleClass().addAll("button", "button-primary");
        saveBtn.setPrefHeight(42);
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> handleSaveExamSettings());

        saveStatusLabel = new Label();
        saveStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        formCard.getChildren().addAll(sectionTitle, sectionDesc, new Separator(), grid, saveBtn, saveStatusLabel);

        // Right Panel: Live Participant Preview Card
        VBox rightBox = new VBox(16);
        rightBox.setMinWidth(340);
        rightBox.setMaxWidth(400);

        Label previewTitle = new Label("Live Participant View");
        previewTitle.getStyleClass().add("title-small");
        Label previewDesc = new Label("This summary reflects what participants will see on their examination portal.");
        previewDesc.getStyleClass().add("subtitle");

        participantPreviewCard = new VBox(14);
        participantPreviewCard.getStyleClass().add("card-accent");
        participantPreviewCard.setPadding(new Insets(20));

        rightBox.getChildren().addAll(previewTitle, previewDesc, participantPreviewCard);

        splitBox.getChildren().addAll(formCard, rightBox);
        container.getChildren().add(splitBox);

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    private Region buildQuestionBankTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setAlignment(Pos.TOP_LEFT);

        VBox card = new VBox(18);
        card.getStyleClass().add("card");
        card.setMaxWidth(760);

        Label cardTitle = new Label("Question Bank Administration");
        cardTitle.getStyleClass().add("title-small");

        Label cardSubtitle = new Label("Maintain, edit, and expand questions stored in the local SQLite database.");
        cardSubtitle.getStyleClass().add("subtitle");

        // Stat Badges
        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        int totalQ = questionDAO.getQuestionCount();
        VBox stat1 = createMiniStat("Total Questions", String.valueOf(totalQ), "#6366f1");
        VBox stat2 = createMiniStat("Categories", String.valueOf(categoryDAO.getAllCategories().size()), "#10b981");

        statsBox.getChildren().addAll(stat1, stat2);

        Label notice = new Label("Click the button below to launch the Question Bank Manager, where you can add new questions, edit existing questions with clear text visibility, delete questions, and batch import from the online OpenTDB API or JSON files.");
        notice.setWrapText(true);
        notice.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-line-spacing: 4px;");

        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button openManagerBtn = new Button("🚀 Open Question Bank Manager");
        openManagerBtn.getStyleClass().addAll("button", "button-primary");
        openManagerBtn.setPrefHeight(44);
        openManagerBtn.setOnAction(e -> NavigationManager.getInstance().showQuestionManagerScreen());

        Button quickApiImportBtn = new Button("⚡ Quick Import from Trivia API");
        quickApiImportBtn.getStyleClass().addAll("button", "button-outline");
        quickApiImportBtn.setPrefHeight(44);
        quickApiImportBtn.setOnAction(e -> showQuickApiImportDialog());

        actionRow.getChildren().addAll(openManagerBtn, quickApiImportBtn);

        card.getChildren().addAll(cardTitle, cardSubtitle, statsBox, new Separator(), notice, actionRow);
        container.getChildren().add(card);

        return container;
    }

    private Region buildHistoryTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setAlignment(Pos.TOP_LEFT);

        VBox card = new VBox(18);
        card.getStyleClass().add("card");
        card.setMaxWidth(760);

        Label cardTitle = new Label("Participant Examination Results & Records");
        cardTitle.getStyleClass().add("title-small");

        Label cardSubtitle = new Label("Monitor past examination attempts, performance metrics, and per-question student answers.");
        cardSubtitle.getStyleClass().add("subtitle");

        // Stat Badges
        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        int totalAttempts = attemptDAO.getTotalAttemptsCount();
        double avgPct = attemptDAO.getAverageScorePercentage();
        double bestPct = attemptDAO.getBestScorePercentage();

        VBox stat1 = createMiniStat("Total Attempts", String.valueOf(totalAttempts), "#6366f1");
        VBox stat2 = createMiniStat("Average Score", String.format("%.1f%%", avgPct), "#38bdf8");
        VBox stat3 = createMiniStat("Highest Score", String.format("%.1f%%", bestPct), "#10b981");

        statsBox.getChildren().addAll(stat1, stat2, stat3);

        Label notice = new Label("Access detailed attempt logs, filter scores by category, inspect individual participant submissions with side-by-side answer keys, or clear old records.");
        notice.setWrapText(true);
        notice.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-line-spacing: 4px;");

        Button openHistoryBtn = new Button("📊 Open Full History & Analytics");
        openHistoryBtn.getStyleClass().addAll("button", "button-primary");
        openHistoryBtn.setPrefHeight(44);
        openHistoryBtn.setOnAction(e -> NavigationManager.getInstance().showHistoryScreen());

        card.getChildren().addAll(cardTitle, cardSubtitle, statsBox, new Separator(), notice, openHistoryBtn);
        container.getChildren().add(card);

        return container;
    }

    private Region buildSecurityTab() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(24));
        container.setAlignment(Pos.TOP_LEFT);

        VBox card = new VBox(18);
        card.getStyleClass().add("card");
        card.setMaxWidth(560);

        Label cardTitle = new Label("Administrator Password & Security");
        cardTitle.getStyleClass().add("title-small");

        Label cardSubtitle = new Label("Change the administrator password required to access this control panel.");
        cardSubtitle.getStyleClass().add("subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);

        Label currentLbl = new Label("Current Password:");
        currentLbl.getStyleClass().add("label-field");
        currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Enter current admin password");
        currentPasswordField.setPrefWidth(300);
        grid.add(currentLbl, 0, 0);
        grid.add(currentPasswordField, 1, 0);

        Label newLbl = new Label("New Password:");
        newLbl.getStyleClass().add("label-field");
        newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password (min 4 characters)");
        newPasswordField.setPrefWidth(300);
        grid.add(newLbl, 0, 1);
        grid.add(newPasswordField, 1, 1);

        Label confirmLbl = new Label("Confirm New Password:");
        confirmLbl.getStyleClass().add("label-field");
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Re-type new password");
        confirmPasswordField.setPrefWidth(300);
        grid.add(confirmLbl, 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        Button updatePasswordBtn = new Button("Update Admin Password");
        updatePasswordBtn.getStyleClass().addAll("button", "button-primary");
        updatePasswordBtn.setPrefHeight(40);
        updatePasswordBtn.setOnAction(e -> handleUpdatePassword());

        passwordStatusLabel = new Label();
        passwordStatusLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        card.getChildren().addAll(cardTitle, cardSubtitle, new Separator(), grid, updatePasswordBtn, passwordStatusLabel);
        container.getChildren().add(card);

        return container;
    }

    private VBox createMiniStat(String label, String value, String hexColor) {
        VBox box = new VBox(2);
        box.getStyleClass().add("card-subtle");
        box.setPadding(new Insets(10, 16, 10, 16));
        box.setMinWidth(140);

        Label v = new Label(value);
        v.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: " + hexColor + ";");
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        box.getChildren().addAll(v, l);
        return box;
    }

    private void loadCurrentSettings() {
        // Load categories
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add(new CategoryOption(0, "All Categories (Mixed)"));
        for (Category c : categoryDAO.getAllCategories()) {
            categoryCombo.getItems().add(new CategoryOption(c.getId(), c.getName()));
        }

        // Selected category
        int activeCatId = settingsDAO.getQuizCategoryId();
        CategoryOption matchedCat = categoryCombo.getItems().stream()
                .filter(c -> c.id == activeCatId)
                .findFirst()
                .orElse(categoryCombo.getItems().get(0));
        categoryCombo.setValue(matchedCat);

        // Time limit
        int timeLimit = settingsDAO.getQuizTimeLimitSeconds();
        timeLimitField.setText(String.valueOf(timeLimit));
        updateTimeFormatted();

        // Question count
        int count = settingsDAO.getQuizQuestionCount();
        if (!questionCountCombo.getItems().contains(count)) {
            questionCountCombo.getItems().add(count);
        }
        questionCountCombo.setValue(count);

        // Difficulty
        difficultyCombo.setValue(settingsDAO.getQuizDifficulty());

        // Source
        String source = settingsDAO.getQuizSource();
        if ("Online Trivia API (OpenTDB)".equalsIgnoreCase(source)) {
            sourceCombo.setValue("Online Trivia API (OpenTDB)");
        } else {
            sourceCombo.setValue("Local Database (SQLite)");
        }

        updateParticipantPreview();
    }

    private void updateTimeFormatted() {
        try {
            int sec = Integer.parseInt(timeLimitField.getText().trim());
            if (sec < 10) sec = 10;
            int mins = sec / 60;
            int remSec = sec % 60;
            timeFormattedLabel.setText(String.format("= %02d:%02d (%d sec)", mins, remSec, sec));
        } catch (NumberFormatException e) {
            timeFormattedLabel.setText("= Invalid");
        }
    }

    private void updateParticipantPreview() {
        if (participantPreviewCard == null) return;

        participantPreviewCard.getChildren().clear();

        Label cardHeader = new Label("Active Exam Configuration");
        cardHeader.getStyleClass().add("title-small");
        cardHeader.setStyle("-fx-text-fill: #38bdf8;");

        int timeSec = 60;
        try {
            timeSec = Math.max(10, Integer.parseInt(timeLimitField.getText().trim()));
        } catch (Exception ignored) {}

        int mins = timeSec / 60;
        int remSec = timeSec % 60;
        String formattedTimer = String.format("%02d:%02d (%d seconds)", mins, remSec, timeSec);

        int count = questionCountCombo.getValue() != null ? questionCountCombo.getValue() : 5;
        CategoryOption cat = categoryCombo.getValue();
        String catName = cat != null ? cat.name : "All Categories";
        Difficulty diff = difficultyCombo.getValue() != null ? difficultyCombo.getValue() : Difficulty.ANY;
        String src = sourceCombo.getValue() != null ? sourceCombo.getValue() : "Local Database (SQLite)";

        VBox itemsBox = new VBox(10);
        itemsBox.getChildren().addAll(
                createPreviewItem("Subject / Category:", catName),
                createPreviewItem("Difficulty Level:", diff.getDisplayName()),
                createPreviewItem("Total Questions:", count + " questions"),
                createPreviewItem("Allocated Timer:", formattedTimer),
                createPreviewItem("Question Source:", src)
        );

        Label lockNotice = new Label("🔒 Timer Lockdown: Timer runs continuously and cannot be paused or changed by participants.");
        lockNotice.setWrapText(true);
        lockNotice.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px; -fx-font-style: italic;");

        participantPreviewCard.getChildren().addAll(cardHeader, new Separator(), itemsBox, new Separator(), lockNotice);
    }

    private HBox createPreviewItem(String label, String value) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);

        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: bold;");
        l.setMinWidth(130);

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px;");

        row.getChildren().addAll(l, v);
        return row;
    }

    private void handleSaveExamSettings() {
        int seconds;
        try {
            seconds = Integer.parseInt(timeLimitField.getText().trim());
            if (seconds < 10) {
                showAlert(Alert.AlertType.WARNING, "Invalid Time", "Exam time limit must be at least 10 seconds.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid Number", "Please enter a valid number of seconds for the time limit.");
            return;
        }

        int count = questionCountCombo.getValue() != null ? questionCountCombo.getValue() : 5;
        CategoryOption cat = categoryCombo.getValue();
        int catId = cat != null ? cat.id : 0;
        String catName = cat != null ? cat.name : "All Categories (Mixed)";
        Difficulty diff = difficultyCombo.getValue() != null ? difficultyCombo.getValue() : Difficulty.ANY;
        String src = sourceCombo.getValue() != null ? sourceCombo.getValue() : "Local Database (SQLite)";

        settingsDAO.setQuizTimeLimitSeconds(seconds);
        settingsDAO.setQuizQuestionCount(count);
        settingsDAO.setQuizCategory(catId, catName);
        settingsDAO.setQuizDifficulty(diff);
        settingsDAO.setQuizSource(src);

        saveStatusLabel.setText("✓ Exam settings successfully saved and activated for all participants!");
        saveStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 13px; -fx-font-weight: bold;");

        updateParticipantPreview();
    }

    private void handleUpdatePassword() {
        String current = currentPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        String actualCurrent = settingsDAO.getAdminPassword();
        if (!actualCurrent.equals(current)) {
            passwordStatusLabel.setText("✗ Incorrect current admin password.");
            passwordStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
            return;
        }

        if (newPass == null || newPass.trim().length() < 4) {
            passwordStatusLabel.setText("✗ New password must be at least 4 characters long.");
            passwordStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
            return;
        }

        if (!newPass.equals(confirm)) {
            passwordStatusLabel.setText("✗ New password and confirmation do not match.");
            passwordStatusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 13px;");
            return;
        }

        settingsDAO.setAdminPassword(newPass.trim());
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmPasswordField.clear();

        passwordStatusLabel.setText("✓ Admin password successfully updated!");
        passwordStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 13px;");
    }

    private void showQuickApiImportDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Quick Import Questions");
        dialog.setHeaderText("Batch import questions from Open Trivia DB directly into SQLite.");

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        ComboBox<Integer> countBox = new ComboBox<>(FXCollections.observableArrayList(5, 10, 15, 20));
        countBox.setValue(10);

        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(
                "Any Category", "Computer Science", "Science & Nature", "History", "General Knowledge"
        ));
        catBox.setValue("Any Category");

        ComboBox<Difficulty> diffBox = new ComboBox<>(FXCollections.observableArrayList(
                Difficulty.ANY, Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD
        ));
        diffBox.setValue(Difficulty.ANY);

        content.getChildren().addAll(
                new Label("Question Count:"), countBox,
                new Label("Category:"), catBox,
                new Label("Difficulty:"), diffBox
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                int amount = countBox.getValue();
                Difficulty diff = diffBox.getValue();
                Integer catId = switch (catBox.getValue()) {
                    case "Computer Science" -> 1;
                    case "Science & Nature" -> 2;
                    case "History" -> 3;
                    case "General Knowledge" -> 4;
                    default -> null;
                };

                apiService.fetchQuestionsAsync(amount, catId, diff)
                        .thenAccept(importedQuestions -> Platform.runLater(() -> {
                            showAlert(Alert.AlertType.INFORMATION, "Import Complete",
                                    "Successfully imported " + importedQuestions.size() +
                                            " questions into your SQLite database.");
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Import Failed", ex.getMessage()));
                            return null;
                        });
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

    private static class CategoryOption {
        final int id;
        final String name;

        CategoryOption(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
