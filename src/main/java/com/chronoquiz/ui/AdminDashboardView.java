package com.chronoquiz.ui;

import com.chronoquiz.api.ApiService;
import com.chronoquiz.db.AttemptDAO;
import com.chronoquiz.db.CategoryDAO;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.db.SettingsDAO;
import com.chronoquiz.model.Category;
import com.chronoquiz.model.Difficulty;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import com.chronoquiz.util.JsonImporter;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Admin Dashboard Screen:
 * Provides password-protected administration for configuring active participant exam parameters
 * (time limit, question count, category, difficulty, source), managing custom question categories,
 * maintaining the question bank, reviewing participant attempt history, and updating administrative credentials.
 */
public class AdminDashboardView {
    private final BorderPane root = new BorderPane();
    private final SettingsDAO settingsDAO = new SettingsDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final AttemptDAO attemptDAO = new AttemptDAO();
    private final ApiService apiService = new ApiService();
    private final JsonImporter jsonImporter = new JsonImporter();

    // Active Exam controls
    private TextField timeLimitField;
    private Label timeFormattedLabel;
    private ComboBox<Integer> questionCountCombo;
    private ComboBox<CategoryOption> categoryCombo;
    private ComboBox<Difficulty> difficultyCombo;
    private ComboBox<String> sourceCombo;
    private Label saveStatusLabel;
    private VBox participantPreviewCard;

    // Category Management controls
    private TableView<Category> categoryTableView;
    private final ObservableList<Category> categoryObservableList = FXCollections.observableArrayList();
    private Label totalCategoriesBadge;
    private Label totalCategorizedQuestionsBadge;
    private Label totalCategoryAttemptsBadge;

    // Password controls
    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;
    private Label passwordStatusLabel;

    public AdminDashboardView() {
        buildUI();
        loadCurrentSettings();
        loadCategoryTableData();
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(24, 40, 30, 40));

        // Top Navigation Bar
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Button exitToPortalBtn = new Button("← Back to Participant Portal");
        exitToPortalBtn.getStyleClass().addAll("button", "button-outline");
        exitToPortalBtn.setOnAction(e -> NavigationManager.getInstance().showStartScreen());

        VBox titleBox = new VBox(3);
        Label title = new Label("Administrator Control Panel");
        title.getStyleClass().add("title-medium");
        Label subtitle = new Label("Set participant exam parameters, lock timers, manage categories, and monitor results.");
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

        // Tab 2: Category & Subject Manager (Admin-Only Feature)
        Tab categoryTab = new Tab("🏷️ Category Manager", buildCategoryManagerTab());

        // Tab 3: Question Bank Manager
        Tab questionTab = new Tab("📚 Question Bank", buildQuestionBankTab());

        // Tab 4: Participant Attempt History
        Tab historyTab = new Tab("📊 Participant History", buildHistoryTab());

        // Tab 5: Security & Credentials
        Tab securityTab = new Tab("🔒 Admin Password", buildSecurityTab());

        tabPane.getTabs().addAll(examTab, categoryTab, questionTab, historyTab, securityTab);
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
        formCard.setMinWidth(520);
        formCard.setMaxWidth(620);
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

        VBox timeInputBox = new VBox(8);
        HBox timeRow = new HBox(12);
        timeRow.setAlignment(Pos.CENTER_LEFT);

        timeLimitField = new TextField();
        timeLimitField.setPromptText("e.g. 60");
        timeLimitField.setPrefWidth(110);
        timeLimitField.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        timeFormattedLabel = new Label("01:00 (60s)");
        timeFormattedLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 13px;");

        timeRow.getChildren().addAll(timeLimitField, timeFormattedLabel);

        // Quick Presets using FlowPane and USE_PREF_SIZE so buttons never truncate with "..."
        FlowPane presetBox = new FlowPane();
        presetBox.setHgap(6);
        presetBox.setVgap(6);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        presetBox.setPadding(new Insets(2, 0, 0, 0));

        Label presetLbl = new Label("Presets:");
        presetLbl.setMinWidth(Region.USE_PREF_SIZE);
        presetLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: bold;");
        presetBox.getChildren().add(presetLbl);

        int[] presets = {30, 45, 60, 90, 120, 180, 300};
        for (int p : presets) {
            Button pBtn = new Button(p + "s");
            pBtn.getStyleClass().setAll("preset-btn");
            pBtn.setMinWidth(Region.USE_PREF_SIZE);
            pBtn.setMinHeight(Region.USE_PREF_SIZE);
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

        // 4. Subject / Category with Quick "+ Add New" button
        Label catLbl = new Label("Exam Category / Subject:");
        catLbl.getStyleClass().add("label-field");

        HBox catInputRow = new HBox(8);
        catInputRow.setAlignment(Pos.CENTER_LEFT);

        categoryCombo = new ComboBox<>();
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        categoryCombo.setOnAction(e -> updateParticipantPreview());
        HBox.setHgrow(categoryCombo, Priority.ALWAYS);

        Button quickAddCatBtn = new Button("➕ Add New");
        quickAddCatBtn.getStyleClass().addAll("button", "button-outline");
        quickAddCatBtn.setStyle("-fx-font-size: 12px; -fx-padding: 7px 12px; -fx-cursor: hand;");
        quickAddCatBtn.setOnAction(e -> showAddCategoryDialog());

        catInputRow.getChildren().addAll(categoryCombo, quickAddCatBtn);
        grid.add(catLbl, 0, 3);
        grid.add(catInputRow, 1, 3);

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
        Button saveBtn = new Button("💾 Save & Apply Exam Settings");
        saveBtn.getStyleClass().addAll("button", "button-primary");
        saveBtn.setPrefHeight(44);
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> handleSaveExamSettings());

        saveStatusLabel = new Label();
        saveStatusLabel.setWrapText(true);

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

    private Region buildCategoryManagerTab() {
        VBox container = new VBox(18);
        container.setPadding(new Insets(20, 10, 20, 10));

        VBox card = new VBox(16);
        card.getStyleClass().add("card");
        VBox.setVgrow(card, Priority.ALWAYS);

        // Header Row
        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(3);
        Label title = new Label("Quiz Categories & Curriculum Subjects");
        title.getStyleClass().add("title-small");
        Label subtitle = new Label("Create, organize, and maintain examination subjects with custom names, curriculum outlines, and question bank metrics.");
        subtitle.getStyleClass().add("subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button addCatBtn = new Button("➕ Add New Category");
        addCatBtn.getStyleClass().addAll("button", "button-success");
        addCatBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 10px 18px; -fx-cursor: hand;");
        addCatBtn.setOnAction(e -> showAddCategoryDialog());

        headerRow.getChildren().addAll(titleBox, headerSpacer, addCatBtn);

        // Stats Row
        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER_LEFT);

        VBox stat1 = createMiniStat("Total Categories", "0", "#a855f7");
        totalCategoriesBadge = (Label) stat1.getChildren().get(1);

        VBox stat2 = createMiniStat("Categorized Questions", "0", "#06b6d4");
        totalCategorizedQuestionsBadge = (Label) stat2.getChildren().get(1);

        VBox stat3 = createMiniStat("Participant Attempts", "0", "#10b981");
        totalCategoryAttemptsBadge = (Label) stat3.getChildren().get(1);

        statsBox.getChildren().addAll(stat1, stat2, stat3);

        // TableView of Categories
        categoryTableView = new TableView<>(categoryObservableList);
        categoryTableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        categoryTableView.setPrefHeight(340);

        TableColumn<Category, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(60);
        colId.setMinWidth(45);

        TableColumn<Category, String> colName = new TableColumn<>("Category / Subject Name");
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colName.setMinWidth(180);

        TableColumn<Category, String> colDesc = new TableColumn<>("Curriculum Scope / Subject Description");
        colDesc.setCellValueFactory(cellData -> {
            String desc = cellData.getValue().getDescription();
            return new SimpleStringProperty(desc == null || desc.isBlank() ? "No description provided" : desc);
        });
        colDesc.setMinWidth(280);

        TableColumn<Category, String> colQCount = new TableColumn<>("Question Bank");
        colQCount.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getQuestionCount() + " Questions"));
        colQCount.setMinWidth(120);

        TableColumn<Category, String> colAttempts = new TableColumn<>("Exam Attempts");
        colAttempts.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAttemptCount() + " Attempts"));
        colAttempts.setMinWidth(120);

        categoryTableView.getColumns().addAll(colId, colName, colDesc, colQCount, colAttempts);

        // Actions Row
        HBox actionsRow = new HBox(12);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        actionsRow.setPadding(new Insets(8, 0, 4, 0));

        Button editCatBtn = new Button("✏ Edit Selected");
        editCatBtn.getStyleClass().addAll("button", "button-primary");
        editCatBtn.setOnAction(e -> showEditCategoryDialog(categoryTableView.getSelectionModel().getSelectedItem()));

        Button deleteCatBtn = new Button("🗑 Delete Selected");
        deleteCatBtn.getStyleClass().addAll("button", "button-danger");
        deleteCatBtn.setOnAction(e -> handleDeleteCategory(categoryTableView.getSelectionModel().getSelectedItem()));

        Button setActiveExamBtn = new Button("🎯 Set as Active Exam Category");
        setActiveExamBtn.getStyleClass().addAll("button", "button-outline");
        setActiveExamBtn.setOnAction(e -> handleSetActiveExamCategory(categoryTableView.getSelectionModel().getSelectedItem()));

        Button manageQuestionsBtn = new Button("📚 Open Question Bank");
        manageQuestionsBtn.getStyleClass().addAll("button", "button-outline");
        manageQuestionsBtn.setOnAction(e -> NavigationManager.getInstance().showQuestionManagerScreen());

        actionsRow.getChildren().addAll(addCatBtn, editCatBtn, deleteCatBtn, setActiveExamBtn, manageQuestionsBtn);

        card.getChildren().addAll(headerRow, statsBox, categoryTableView, actionsRow);
        container.getChildren().add(card);

        ScrollPane scroll = new ScrollPane(container);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        return scroll;
    }

    private void loadCategoryTableData() {
        List<Category> cats = categoryDAO.getAllCategories();
        categoryObservableList.clear();
        categoryObservableList.addAll(cats);

        int totalQ = 0;
        int totalA = 0;
        for (Category c : cats) {
            totalQ += c.getQuestionCount();
            totalA += c.getAttemptCount();
        }

        if (totalCategoriesBadge != null) {
            totalCategoriesBadge.setText(String.valueOf(cats.size()));
        }
        if (totalCategorizedQuestionsBadge != null) {
            totalCategorizedQuestionsBadge.setText(String.valueOf(totalQ));
        }
        if (totalCategoryAttemptsBadge != null) {
            totalCategoryAttemptsBadge.setText(String.valueOf(totalA));
        }
    }

    public void showAddCategoryDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New Quiz Category");
        dialog.setHeaderText("Create a new quiz category with custom name and curriculum details.");

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setMinWidth(440);

        Label nameLbl = new Label("Category / Subject Name (Required):");
        nameLbl.getStyleClass().add("label-field");
        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Software Engineering, Organic Chemistry, World History");
        nameField.setStyle("-fx-control-inner-background: #0b1329; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b; -fx-font-size: 14px;");

        Label descLbl = new Label("Subject Curriculum / Description Outline:");
        descLbl.getStyleClass().add("label-field");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Enter a brief description of the syllabus coverage, topics, or difficulty expectations...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-control-inner-background: #0b1329; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b; -fx-font-size: 13px;");

        box.getChildren().addAll(nameLbl, nameField, descLbl, descArea);
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #131b2e;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String name = nameField.getText().trim();
            String desc = descArea.getText().trim();

            if (name.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Category name cannot be empty.");
                return;
            }

            if (categoryDAO.categoryExists(name)) {
                showAlert(Alert.AlertType.WARNING, "Duplicate Category", "A category named '" + name + "' already exists.");
                return;
            }

            int newId = categoryDAO.createCategory(name, desc);
            if (newId > 0) {
                showAlert(Alert.AlertType.INFORMATION, "Category Created",
                        "Successfully created category: " + name +
                                "\nThis category is now available across exams, questions, and participant portals.");
                loadCategoryTableData();
                loadCurrentSettings();

                // Select the new category in the exam dropdown
                for (CategoryOption opt : categoryCombo.getItems()) {
                    if (opt.id == newId) {
                        categoryCombo.setValue(opt);
                        break;
                    }
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Creation Failed", "Could not create category in database.");
            }
        }
    }

    private void showEditCategoryDialog(Category selected) {
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a category from the table to edit.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Quiz Category");
        dialog.setHeaderText("Update details for category: " + selected.getName());

        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.setMinWidth(440);

        Label nameLbl = new Label("Category / Subject Name:");
        nameLbl.getStyleClass().add("label-field");
        TextField nameField = new TextField(selected.getName());
        nameField.setStyle("-fx-control-inner-background: #0b1329; -fx-text-fill: #ffffff; -fx-font-size: 14px;");

        Label descLbl = new Label("Subject Curriculum / Description Outline:");
        descLbl.getStyleClass().add("label-field");
        TextArea descArea = new TextArea(selected.getDescription());
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        descArea.setStyle("-fx-control-inner-background: #0b1329; -fx-text-fill: #ffffff; -fx-font-size: 13px;");

        box.getChildren().addAll(nameLbl, nameField, descLbl, descArea);
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #131b2e;");

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String name = nameField.getText().trim();
            String desc = descArea.getText().trim();

            if (name.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Category name cannot be empty.");
                return;
            }

            boolean success = categoryDAO.updateCategory(selected.getId(), name, desc);
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Category Updated", "Successfully updated category: " + name);
                loadCategoryTableData();
                loadCurrentSettings();
            } else {
                showAlert(Alert.AlertType.ERROR, "Update Failed", "Could not update category. The name might conflict with another category.");
            }
        }
    }

    private void handleDeleteCategory(Category selected) {
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a category from the table to delete.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Category Deletion");
        alert.setHeaderText("Delete category '" + selected.getName() + "'?");
        alert.setContentText("This category currently contains " + selected.getQuestionCount() + " questions and has " +
                selected.getAttemptCount() + " exam attempts recorded.\n\n" +
                "Deleting this category will unlink these questions and attempts (setting their category to unassigned/mixed).\n\n" +
                "Are you sure you want to proceed?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = categoryDAO.deleteCategory(selected.getId());
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Category Deleted", "Category '" + selected.getName() + "' has been deleted.");
                loadCategoryTableData();
                loadCurrentSettings();
            } else {
                showAlert(Alert.AlertType.ERROR, "Deletion Failed", "Failed to delete category.");
            }
        }
    }

    private void handleSetActiveExamCategory(Category selected) {
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a category from the table.");
            return;
        }

        settingsDAO.setQuizCategory(selected.getId(), selected.getName());
        loadCurrentSettings();
        showAlert(Alert.AlertType.INFORMATION, "Active Exam Subject Set",
                "The active exam subject has been set to: " + selected.getName() +
                        "\nParticipants starting an exam will be tested on this category.");
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

        int totalQuestions = questionDAO.getQuestionCount();
        int totalCategories = categoryDAO.getCategoryCount();

        VBox stat1 = createMiniStat("Total Questions", String.valueOf(totalQuestions), "#6366f1");
        VBox stat2 = createMiniStat("Active Subjects", String.valueOf(totalCategories), "#06b6d4");
        VBox stat3 = createMiniStat("Question Types", "MCQ & Short Answer", "#10b981");

        statsBox.getChildren().addAll(stat1, stat2, stat3);

        Label notice = new Label("Open the complete Question Bank Manager to insert custom questions, modify existing prompts, import trivia packs from OpenTDB API, or import/export question sets via JSON.");
        notice.setWrapText(true);
        notice.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-line-spacing: 4px;");

        HBox actionRow = new HBox(14);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Button openManagerBtn = new Button("📚 Open Full Question Bank Manager");
        openManagerBtn.getStyleClass().addAll("button", "button-primary");
        openManagerBtn.setPrefHeight(44);
        openManagerBtn.setOnAction(e -> NavigationManager.getInstance().showQuestionManagerScreen());

        Button quickApiImportBtn = new Button("⚡ Quick Import from Trivia API");
        quickApiImportBtn.getStyleClass().addAll("button", "button-outline");
        quickApiImportBtn.setPrefHeight(44);
        quickApiImportBtn.setOnAction(e -> showQuickApiImportDialog());

        Button quickJsonImportBtn = new Button("📥 Import from JSON File");
        quickJsonImportBtn.getStyleClass().addAll("button", "button-outline");
        quickJsonImportBtn.setPrefHeight(44);
        quickJsonImportBtn.setOnAction(e -> handleJsonImport());

        actionRow.getChildren().addAll(openManagerBtn, quickApiImportBtn, quickJsonImportBtn);

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
        currentPasswordField.setPromptText("Enter current password");
        grid.add(currentLbl, 0, 0);
        grid.add(currentPasswordField, 1, 0);

        Label newLbl = new Label("New Password:");
        newLbl.getStyleClass().add("label-field");
        newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Enter new password (min 4 chars)");
        grid.add(newLbl, 0, 1);
        grid.add(newPasswordField, 1, 1);

        Label confirmLbl = new Label("Confirm Password:");
        confirmLbl.getStyleClass().add("label-field");
        confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Re-type new password");
        grid.add(confirmLbl, 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        Button updatePassBtn = new Button("🔐 Update Admin Password");
        updatePassBtn.getStyleClass().addAll("button", "button-primary");
        updatePassBtn.setPrefHeight(40);
        updatePassBtn.setOnAction(e -> handleUpdatePassword());

        passwordStatusLabel = new Label();
        passwordStatusLabel.setWrapText(true);

        card.getChildren().addAll(cardTitle, cardSubtitle, new Separator(), grid, updatePassBtn, passwordStatusLabel);
        container.getChildren().add(card);

        return container;
    }

    private VBox createMiniStat(String label, String value, String colorHex) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card-subtle");
        box.setPadding(new Insets(12, 16, 12, 16));
        box.setMinWidth(140);

        Label l = new Label(label);
        l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold;");

        Label v = new Label(value);
        v.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-size: 20px; -fx-font-weight: 900;");

        box.getChildren().addAll(l, v);
        return box;
    }

    private void loadCurrentSettings() {
        // Load categories
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add(new CategoryOption(0, "All Categories (Mixed)", "Questions drawn from across all available subjects and question banks."));
        for (Category c : categoryDAO.getAllCategories()) {
            categoryCombo.getItems().add(new CategoryOption(c.getId(), c.getName(), c.getDescription()));
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
            String text = timeLimitField.getText();
            if (text == null || text.trim().isEmpty()) {
                timeFormattedLabel.setText("--:--");
                return;
            }
            int sec = Integer.parseInt(text.trim());
            if (sec < 0) sec = 0;
            int mins = sec / 60;
            int remSec = sec % 60;
            timeFormattedLabel.setText(String.format("%02d:%02d (%ds)", mins, remSec, sec));
        } catch (NumberFormatException e) {
            timeFormattedLabel.setText("--:--");
        }
    }

    private void updateParticipantPreview() {
        if (participantPreviewCard == null) return;
        participantPreviewCard.getChildren().clear();

        Label cardHeader = new Label("Active Participant Parameters");
        cardHeader.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #38bdf8;");

        int timeSec;
        try {
            timeSec = Integer.parseInt(timeLimitField.getText().trim());
            if (timeSec < 10) timeSec = 10;
        } catch (Exception e) {
            timeSec = 60;
        }

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

        passwordStatusLabel.setText("✓ Admin password updated successfully!");
        passwordStatusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 13px; -fx-font-weight: bold;");
    }

    private void showQuickApiImportDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Quick Import from OpenTDB Trivia API");
        dialog.setHeaderText("Fetch questions from the online trivia API into SQLite");

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

        VBox content = new VBox(10,
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
                            loadCategoryTableData();
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Import Failed", ex.getMessage()));
                            return null;
                        });
            }
        });
    }

    private void handleJsonImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Questions from JSON File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showOpenDialog(NavigationManager.getInstance().getPrimaryStage());
        if (file != null) {
            try {
                int imported = jsonImporter.importQuestionsFromFile(file);
                showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                        "Successfully imported " + imported + " new questions from JSON into SQLite storage.");
                loadCategoryTableData();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Import Error", "Failed to parse JSON file: " + e.getMessage());
            }
        }
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
        final String description;

        CategoryOption(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description != null ? description : "";
        }

        CategoryOption(int id, String name) {
            this(id, name, "");
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
