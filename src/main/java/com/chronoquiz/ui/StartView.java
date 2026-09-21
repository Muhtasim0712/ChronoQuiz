package com.chronoquiz.ui;

import com.chronoquiz.api.ApiService;
import com.chronoquiz.db.CategoryDAO;
import com.chronoquiz.db.QuestionDAO;
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

/**
 * Start Screen: Configures quiz parameters, selects question source,
 * and handles background API fetches with offline fallback.
 */
public class StartView {
    private final BorderPane root = new BorderPane();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final ApiService apiService = new ApiService();

    private TextField nameField;
    private ComboBox<String> sourceCombo;
    private ComboBox<CategoryItem> categoryCombo;
    private ComboBox<Difficulty> difficultyCombo;
    private ComboBox<Integer> amountCombo;
    private Label timeEstimateLabel;
    private Label totalAvailableLabel;
    private Button startBtn;
    private ProgressIndicator loadingIndicator;

    public StartView() {
        buildUI();
        loadCategories();
        updateTimeEstimate();
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(30, 50, 40, 50));

        // Header
        VBox headerBox = new VBox(6);
        headerBox.setAlignment(Pos.CENTER);

        Label title = new Label("ChronoQuiz");
        title.getStyleClass().add("title-large");

        Label subtitle = new Label("High-Precision Timed Desktop Quiz & Examination System");
        subtitle.getStyleClass().add("subtitle");

        headerBox.getChildren().addAll(title, subtitle);
        root.setTop(headerBox);

        // Center: Settings Card
        VBox centerCard = new VBox(20);
        centerCard.getStyleClass().add("card-accent");
        centerCard.setMaxWidth(680);
        centerCard.setAlignment(Pos.CENTER_LEFT);

        Label setupTitle = new Label("Quiz Configuration");
        setupTitle.getStyleClass().add("title-medium");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(20);
        formGrid.setVgap(16);
        formGrid.setAlignment(Pos.CENTER);

        // Row 0: Player Name
        Label nameLbl = new Label("Player Name:");
        nameLbl.getStyleClass().add("label-field");
        nameField = new TextField("Student");
        nameField.setPromptText("Enter your name");
        nameField.setPrefWidth(320);
        formGrid.add(nameLbl, 0, 0);
        formGrid.add(nameField, 1, 0);

        // Row 1: Question Source
        Label sourceLbl = new Label("Question Source:");
        sourceLbl.getStyleClass().add("label-field");
        sourceCombo = new ComboBox<>(FXCollections.observableArrayList(
                "Local Database (SQLite)",
                "Online Trivia API (OpenTDB)"
        ));
        sourceCombo.setValue("Local Database (SQLite)");
        sourceCombo.setPrefWidth(320);
        formGrid.add(sourceLbl, 0, 1);
        formGrid.add(sourceCombo, 1, 1);

        // Row 2: Category
        Label catLbl = new Label("Category:");
        catLbl.getStyleClass().add("label-field");
        categoryCombo = new ComboBox<>();
        categoryCombo.setPrefWidth(320);
        formGrid.add(catLbl, 0, 2);
        formGrid.add(categoryCombo, 1, 2);

        // Row 3: Difficulty
        Label diffLbl = new Label("Difficulty Level:");
        diffLbl.getStyleClass().add("label-field");
        difficultyCombo = new ComboBox<>(FXCollections.observableArrayList(
                Difficulty.ANY,
                Difficulty.EASY,
                Difficulty.MEDIUM,
                Difficulty.HARD
        ));
        difficultyCombo.setValue(Difficulty.ANY);
        difficultyCombo.setPrefWidth(320);
        formGrid.add(diffLbl, 0, 3);
        formGrid.add(difficultyCombo, 1, 3);

        // Row 4: Number of Questions
        Label amountLbl = new Label("Question Count:");
        amountLbl.getStyleClass().add("label-field");
        amountCombo = new ComboBox<>(FXCollections.observableArrayList(5, 10, 15, 20));
        amountCombo.setValue(5);
        amountCombo.setPrefWidth(320);
        formGrid.add(amountLbl, 0, 4);
        formGrid.add(amountCombo, 1, 4);

        // Time Estimate & Available Questions summary
        HBox metaBox = new HBox(30);
        metaBox.setAlignment(Pos.CENTER_LEFT);
        metaBox.setPadding(new Insets(10, 0, 0, 0));

        timeEstimateLabel = new Label("Estimated Time: 60s");
        timeEstimateLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");

        totalAvailableLabel = new Label();
        totalAvailableLabel.setStyle("-fx-text-fill: #94a3b8;");

        metaBox.getChildren().addAll(timeEstimateLabel, totalAvailableLabel);

        amountCombo.setOnAction(e -> updateTimeEstimate());

        // Buttons Box
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(15, 0, 0, 0));

        startBtn = new Button("Start Quiz Now");
        startBtn.getStyleClass().addAll("button", "button-primary");
        startBtn.setPrefWidth(220);
        startBtn.setOnAction(e -> handleStartQuiz());

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(28, 28);
        loadingIndicator.setVisible(false);

        actionBox.getChildren().addAll(startBtn, loadingIndicator);

        centerCard.getChildren().addAll(setupTitle, formGrid, metaBox, actionBox);

        StackPane centerWrapper = new StackPane(centerCard);
        centerWrapper.setAlignment(Pos.CENTER);
        root.setCenter(centerWrapper);

        // Bottom Navigation: Question Bank & History
        HBox bottomNav = new HBox(20);
        bottomNav.setAlignment(Pos.CENTER);
        bottomNav.setPadding(new Insets(25, 0, 0, 0));

        Button questionManagerBtn = new Button("Manage Question Bank");
        questionManagerBtn.getStyleClass().addAll("button", "button-outline");
        questionManagerBtn.setOnAction(e -> NavigationManager.getInstance().showQuestionManagerScreen());

        Button historyBtn = new Button("Attempt History & Statistics");
        historyBtn.getStyleClass().addAll("button", "button-outline");
        historyBtn.setOnAction(e -> NavigationManager.getInstance().showHistoryScreen());

        bottomNav.getChildren().addAll(questionManagerBtn, historyBtn);
        root.setBottom(bottomNav);
    }

    private void loadCategories() {
        List<Category> list = categoryDAO.getAllCategories();
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add(new CategoryItem(0, "All Categories (Mixed)"));
        for (Category c : list) {
            categoryCombo.getItems().add(new CategoryItem(c.getId(), c.getName()));
        }
        categoryCombo.getSelectionModel().selectFirst();
    }

    private void updateTimeEstimate() {
        int amount = amountCombo.getValue() != null ? amountCombo.getValue() : 5;
        // 12 seconds allocated per question
        int totalSeconds = amount * 12;
        int mins = totalSeconds / 60;
        int secs = totalSeconds % 60;
        timeEstimateLabel.setText(String.format("Time Limit: %02d:%02d (%d seconds total)", mins, secs, totalSeconds));

        int dbCount = questionDAO.getQuestionCount();
        totalAvailableLabel.setText("Database Bank: " + dbCount + " questions loaded");
    }

    private void handleStartQuiz() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            name = "Player";
        }

        CategoryItem selectedCat = categoryCombo.getValue();
        Integer catId = (selectedCat != null && selectedCat.id > 0) ? selectedCat.id : null;
        String catName = (selectedCat != null) ? selectedCat.name : "All Categories";
        Difficulty difficulty = difficultyCombo.getValue();
        int amount = amountCombo.getValue();
        int timeLimitSeconds = amount * 12;

        boolean isOnline = "Online Trivia API (OpenTDB)".equals(sourceCombo.getValue());

        if (isOnline) {
            fetchOnlineAndStart(name, catId, catName, difficulty, amount, timeLimitSeconds);
        } else {
            startLocalQuiz(name, catId, catName, difficulty, amount, timeLimitSeconds);
        }
    }

    private void startLocalQuiz(String name, Integer catId, String catName, Difficulty difficulty, int amount, int timeLimit) {
        List<Question> questions = questionDAO.getQuestions(catId, difficulty, amount);

        if (questions.isEmpty()) {
            questions = questionDAO.getQuestions(null, Difficulty.ANY, amount);
        }

        if (questions.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Questions Available", "The local database question bank is empty. Please add questions in Question Manager or import from API.");
            return;
        }

        Quiz quiz = new Quiz(name, catName, catId != null ? catId : 0, difficulty, questions, timeLimit);
        NavigationManager.getInstance().showQuizScreen(quiz);
    }

    private void fetchOnlineAndStart(String name, Integer catId, String catName, Difficulty difficulty, int amount, int timeLimit) {
        startBtn.setDisable(true);
        loadingIndicator.setVisible(true);

        apiService.fetchQuestionsAsync(amount, catId, difficulty)
                .thenAccept(questions -> Platform.runLater(() -> {
                    startBtn.setDisable(false);
                    loadingIndicator.setVisible(false);

                    Quiz quiz = new Quiz(name, catName, catId != null ? catId : 0, difficulty, questions, timeLimit);
                    NavigationManager.getInstance().showQuizScreen(quiz);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        startBtn.setDisable(false);
                        loadingIndicator.setVisible(false);

                        showAlert(Alert.AlertType.WARNING, "Online API Offline / Fallback",
                                "Could not fetch questions from Open Trivia DB: " + ex.getMessage() +
                                        "\n\nFalling back seamlessly to local SQLite question bank.");

                        startLocalQuiz(name, catId, catName, difficulty, amount, timeLimit);
                    });
                    return null;
                });
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

        CategoryItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
