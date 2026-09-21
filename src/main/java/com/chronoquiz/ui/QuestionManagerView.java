package com.chronoquiz.ui;

import com.chronoquiz.api.ApiService;
import com.chronoquiz.db.CategoryDAO;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.*;
import com.chronoquiz.util.JsonExporter;
import com.chronoquiz.util.JsonImporter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Question Manager Screen: Provides comprehensive CRUD operations for questions in SQLite,
 * integrates online API batch import with duplicate skipping, and JSON import/export.
 */
public class QuestionManagerView {
    private final BorderPane root = new BorderPane();
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ApiService apiService = new ApiService();
    private final JsonExporter jsonExporter = new JsonExporter();
    private final JsonImporter jsonImporter = new JsonImporter();

    private final ObservableList<Question> questionList = FXCollections.observableArrayList();
    private TableView<Question> tableView;

    // Form inputs
    private ComboBox<String> categoryCombo;
    private ComboBox<String> typeCombo;
    private ComboBox<Difficulty> difficultyCombo;
    private TextArea textArea;
    private TextField correctAnswerField;
    private TextField optionsOrAcceptedField;
    private Label optionsFieldLabel;

    private Question selectedQuestion = null;

    public QuestionManagerView() {
        buildUI();
        loadTableData();
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(20, 30, 20, 30));

        // Top Bar
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Admin Dashboard");
        backBtn.getStyleClass().addAll("button", "button-outline");
        backBtn.setOnAction(e -> NavigationManager.getInstance().showAdminDashboard());

        VBox titleBox = new VBox(2);
        Label title = new Label("Question Bank Manager");
        title.getStyleClass().add("title-medium");
        Label subtitle = new Label("Add, edit, delete, or import questions with SQLite persistence.");
        subtitle.getStyleClass().add("subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Button apiImportBtn = new Button("⚡ Import from Online API");
        apiImportBtn.getStyleClass().addAll("button", "button-primary");
        apiImportBtn.setOnAction(e -> showApiImportDialog());

        Button jsonImportBtn = new Button("Import JSON");
        jsonImportBtn.getStyleClass().addAll("button", "button-outline");
        jsonImportBtn.setOnAction(e -> handleJsonImport());

        Button jsonExportBtn = new Button("Export JSON");
        jsonExportBtn.getStyleClass().addAll("button", "button-outline");
        jsonExportBtn.setOnAction(e -> handleJsonExport());

        topBox.getChildren().addAll(backBtn, titleBox, topSpacer, apiImportBtn, jsonImportBtn, jsonExportBtn);
        root.setTop(topBox);

        // Center: Split layout with TableView and Edit Form
        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-box-border: transparent; -fx-background-color: transparent;");

        // TableView setup
        tableView = new TableView<>(questionList);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Question, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50);
        colId.setMinWidth(40);

        TableColumn<Question, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCat.setMinWidth(110);

        TableColumn<Question, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colType.setMinWidth(110);

        TableColumn<Question, String> colDiff = new TableColumn<>("Difficulty");
        colDiff.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDifficulty().getDisplayName()));
        colDiff.setMinWidth(80);

        TableColumn<Question, String> colText = new TableColumn<>("Question Text");
        colText.setCellValueFactory(new PropertyValueFactory<>("text"));
        colText.setMinWidth(220);

        TableColumn<Question, String> colAnswer = new TableColumn<>("Correct Answer");
        colAnswer.setCellValueFactory(new PropertyValueFactory<>("correctAnswer"));
        colAnswer.setMinWidth(120);

        tableView.getColumns().addAll(colId, colCat, colType, colDiff, colText, colAnswer);

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                populateForm(newVal);
            }
        });

        VBox tableBox = new VBox(10, tableView);
        tableBox.setPadding(new Insets(15, 10, 10, 0));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Right side: Form Card
        VBox formCard = new VBox(14);
        formCard.getStyleClass().add("card");
        formCard.setMinWidth(360);
        formCard.setMaxWidth(420);
        formCard.setPadding(new Insets(18));

        Label formTitle = new Label("Question Editor");
        formTitle.getStyleClass().add("title-small");

        // Category
        Label catLbl = new Label("Category (Select or Type):");
        catLbl.getStyleClass().add("label-field");
        categoryCombo = new ComboBox<>();
        categoryCombo.setEditable(true);
        categoryCombo.setMaxWidth(Double.MAX_VALUE);

        // Type
        Label typeLbl = new Label("Question Type:");
        typeLbl.getStyleClass().add("label-field");
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(
                Question.TYPE_MULTIPLE_CHOICE,
                Question.TYPE_SHORT_ANSWER
        ));
        typeCombo.setValue(Question.TYPE_MULTIPLE_CHOICE);
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        typeCombo.setOnAction(e -> updateFieldLabelsForType());

        // Difficulty
        Label diffLbl = new Label("Difficulty:");
        diffLbl.getStyleClass().add("label-field");
        difficultyCombo = new ComboBox<>(FXCollections.observableArrayList(
                Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD
        ));
        difficultyCombo.setValue(Difficulty.MEDIUM);
        difficultyCombo.setMaxWidth(Double.MAX_VALUE);

        // Question Text
        Label textLbl = new Label("Question Text:");
        textLbl.getStyleClass().add("label-field");
        textArea = new TextArea();
        textArea.setPromptText("Enter question prompt here...");
        textArea.setPrefRowCount(3);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-control-inner-background: #0b1329; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b; -fx-font-size: 14px; -fx-font-weight: 500;");

        // Correct Answer
        Label ansLbl = new Label("Primary Correct Answer:");
        ansLbl.getStyleClass().add("label-field");
        correctAnswerField = new TextField();
        correctAnswerField.setPromptText("Exact correct answer");
        correctAnswerField.setStyle("-fx-control-inner-background: #0b1329; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b; -fx-font-size: 14px;");

        // Options or Accepted Answers
        optionsFieldLabel = new Label("Options (comma-separated):");
        optionsFieldLabel.getStyleClass().add("label-field");
        optionsOrAcceptedField = new TextField();
        optionsOrAcceptedField.setPromptText("Option 1, Option 2, Option 3, Option 4");
        optionsOrAcceptedField.setStyle("-fx-control-inner-background: #0b1329; -fx-text-fill: #ffffff; -fx-prompt-text-fill: #64748b; -fx-font-size: 14px;");

        // Form Action Buttons
        HBox btnRow1 = new HBox(10);
        btnRow1.setAlignment(Pos.CENTER);

        Button addBtn = new Button("Add New");
        addBtn.getStyleClass().addAll("button", "button-success");
        addBtn.setOnAction(e -> handleAddQuestion());

        Button updateBtn = new Button("Save Changes");
        updateBtn.getStyleClass().addAll("button", "button-primary");
        updateBtn.setOnAction(e -> handleUpdateQuestion());

        btnRow1.getChildren().addAll(addBtn, updateBtn);

        HBox btnRow2 = new HBox(10);
        btnRow2.setAlignment(Pos.CENTER);

        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().addAll("button", "button-danger");
        deleteBtn.setOnAction(e -> handleDeleteQuestion());

        Button clearBtn = new Button("Clear Form");
        clearBtn.getStyleClass().addAll("button", "button-outline");
        clearBtn.setOnAction(e -> clearForm());

        btnRow2.getChildren().addAll(deleteBtn, clearBtn);

        formCard.getChildren().addAll(
                formTitle,
                catLbl, categoryCombo,
                typeLbl, typeCombo,
                diffLbl, difficultyCombo,
                textLbl, textArea,
                ansLbl, correctAnswerField,
                optionsFieldLabel, optionsOrAcceptedField,
                new Separator(),
                btnRow1, btnRow2
        );

        ScrollPane formScroll = new ScrollPane(formCard);
        formScroll.setFitToWidth(true);
        formScroll.setStyle("-fx-background-color: transparent;");

        splitPane.getItems().addAll(tableBox, formScroll);
        splitPane.setDividerPositions(0.62);

        root.setCenter(splitPane);
    }

    private void loadTableData() {
        questionList.clear();
        questionList.addAll(questionDAO.getAllQuestions());

        categoryCombo.getItems().clear();
        for (Category c : categoryDAO.getAllCategories()) {
            categoryCombo.getItems().add(c.getName());
        }
        if (!categoryCombo.getItems().isEmpty()) {
            categoryCombo.getSelectionModel().selectFirst();
        }
    }

    private void updateFieldLabelsForType() {
        if (Question.TYPE_SHORT_ANSWER.equals(typeCombo.getValue())) {
            optionsFieldLabel.setText("Alternative Accepted Answers (comma-separated):");
            optionsOrAcceptedField.setPromptText("e.g. Washington, George Washington");
        } else {
            optionsFieldLabel.setText("Options (comma-separated):");
            optionsOrAcceptedField.setPromptText("e.g. Mercury, Venus, Earth, Mars");
        }
    }

    private void populateForm(Question q) {
        this.selectedQuestion = q;
        categoryCombo.setValue(q.getCategoryName());
        typeCombo.setValue(q.getType());
        difficultyCombo.setValue(q.getDifficulty());
        textArea.setText(q.getText());
        correctAnswerField.setText(q.getCorrectAnswer());

        if (q instanceof MultipleChoiceQuestion mcq) {
            List<String> optTexts = new ArrayList<>();
            for (Option opt : mcq.getOptions()) {
                optTexts.add(opt.getOptionText());
            }
            optionsOrAcceptedField.setText(String.join(", ", optTexts));
        } else if (q instanceof ShortAnswerQuestion saq) {
            optionsOrAcceptedField.setText(saq.getAcceptedAnswersAsCsv());
        }

        updateFieldLabelsForType();
    }

    private void clearForm() {
        selectedQuestion = null;
        tableView.getSelectionModel().clearSelection();
        textArea.clear();
        correctAnswerField.clear();
        optionsOrAcceptedField.clear();
        if (!categoryCombo.getItems().isEmpty()) {
            categoryCombo.getSelectionModel().selectFirst();
        }
    }

    private void handleAddQuestion() {
        String text = textArea.getText().trim();
        String correct = correctAnswerField.getText().trim();
        String catName = categoryCombo.getValue() != null ? categoryCombo.getValue().trim() : "General Knowledge";
        if (catName.isEmpty()) catName = "General Knowledge";

        if (text.isEmpty() || correct.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Form", "Please provide both question text and a correct answer.");
            return;
        }

        int catId = categoryDAO.getOrCreateCategory(catName);
        Difficulty diff = difficultyCombo.getValue() != null ? difficultyCombo.getValue() : Difficulty.MEDIUM;
        String type = typeCombo.getValue();

        Question newQuestion;
        if (Question.TYPE_SHORT_ANSWER.equals(type)) {
            ShortAnswerQuestion saq = new ShortAnswerQuestion(0, catId, catName, text, diff, correct, null);
            saq.setAcceptedAnswersFromCsv(optionsOrAcceptedField.getText());
            newQuestion = saq;
        } else {
            List<Option> options = parseOptions(optionsOrAcceptedField.getText(), correct);
            newQuestion = new MultipleChoiceQuestion(0, catId, catName, text, diff, correct, options);
        }

        if (questionDAO.insertQuestion(newQuestion)) {
            loadTableData();
            clearForm();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Question successfully added to database.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to insert question into database.");
        }
    }

    private void handleUpdateQuestion() {
        if (selectedQuestion == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a question from the table to edit.");
            return;
        }

        String text = textArea.getText().trim();
        String correct = correctAnswerField.getText().trim();
        String catName = categoryCombo.getValue() != null ? categoryCombo.getValue().trim() : "General Knowledge";

        if (text.isEmpty() || correct.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Incomplete Form", "Please provide question text and correct answer.");
            return;
        }

        int catId = categoryDAO.getOrCreateCategory(catName);
        selectedQuestion.setText(text);
        selectedQuestion.setCorrectAnswer(correct);
        selectedQuestion.setCategoryId(catId);
        selectedQuestion.setCategoryName(catName);
        selectedQuestion.setDifficulty(difficultyCombo.getValue());
        selectedQuestion.setType(typeCombo.getValue());

        if (selectedQuestion instanceof MultipleChoiceQuestion mcq) {
            List<Option> options = parseOptions(optionsOrAcceptedField.getText(), correct);
            mcq.setOptions(options);
        } else if (selectedQuestion instanceof ShortAnswerQuestion saq) {
            saq.setAcceptedAnswersFromCsv(optionsOrAcceptedField.getText());
        }

        if (questionDAO.updateQuestion(selectedQuestion)) {
            loadTableData();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Question successfully updated.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to update question in database.");
        }
    }

    private void handleDeleteQuestion() {
        Question selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a question to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Question #" + selected.getId() + "?");
        confirm.setContentText("This will permanently delete the question and all associated options from the SQLite database.");

        confirm.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                if (questionDAO.deleteQuestion(selected.getId())) {
                    loadTableData();
                    clearForm();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete question.");
                }
            }
        });
    }

    private List<Option> parseOptions(String rawOptions, String correctAnswer) {
        List<Option> list = new ArrayList<>();
        if (rawOptions != null && !rawOptions.trim().isEmpty()) {
            String[] parts = rawOptions.split(",");
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isEmpty()) {
                    boolean isCorrect = trimmed.equalsIgnoreCase(correctAnswer.trim());
                    list.add(new Option(trimmed, isCorrect));
                }
            }
        }

        boolean hasCorrect = list.stream().anyMatch(Option::isCorrect);
        if (!hasCorrect && !correctAnswer.isEmpty()) {
            list.add(new Option(correctAnswer.trim(), true));
        }
        return list;
    }

    private void showApiImportDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Import Questions from Trivia API");
        dialog.setHeaderText("Fetch questions from Open Trivia DB and save to local SQLite bank.");

        VBox content = new VBox(14);
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

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
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
                            loadTableData();
                            showAlert(Alert.AlertType.INFORMATION, "Import Complete",
                                    "Successfully imported " + importedQuestions.size() +
                                            " questions from Open Trivia DB into SQLite database.");
                        }))
                        .exceptionally(ex -> {
                            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "API Import Failed", ex.getMessage()));
                            return null;
                        });
            }
        });
    }

    private void handleJsonImport() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Questions from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showOpenDialog(NavigationManager.getInstance().getPrimaryStage());
        if (file != null) {
            try {
                int count = jsonImporter.importQuestionsFromFile(file);
                loadTableData();
                showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                        "Successfully imported " + count + " questions into your SQLite database.");
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Import Error", "Failed to parse or import JSON file: " + e.getMessage());
            }
        }
    }

    private void handleJsonExport() {
        List<Question> all = questionDAO.getAllQuestions();
        if (all.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Empty Bank", "No questions available to export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Questions to JSON");
        fileChooser.setInitialFileName("chronoquiz_bank.json");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showSaveDialog(NavigationManager.getInstance().getPrimaryStage());
        if (file != null) {
            try {
                jsonExporter.exportQuestionsToFile(all, file);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                        "Question bank successfully exported to:\n" + file.getAbsolutePath());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Export Error", "Failed to write JSON file: " + e.getMessage());
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
}
