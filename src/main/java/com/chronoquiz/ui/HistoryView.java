package com.chronoquiz.ui;

import com.chronoquiz.db.AttemptDAO;
import com.chronoquiz.db.CategoryDAO;
import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.Category;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Attempt History & Statistics Screen:
 * Displays past quiz attempts loaded from SQLite using asynchronous JavaFX Tasks,
 * provides category filters, and calculates summary metrics.
 */
public class HistoryView {
    private final BorderPane root = new BorderPane();
    private final AttemptDAO attemptDAO = new AttemptDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ChronoQuiz-History-DB-Thread");
        t.setDaemon(true);
        return t;
    });

    private final ObservableList<Attempt> attemptsList = FXCollections.observableArrayList();
    private TableView<Attempt> tableView;
    private ComboBox<CategoryFilterItem> categoryFilterCombo;

    // Stat cards
    private Label totalAttemptsLabel;
    private Label bestScoreLabel;
    private Label avgScoreLabel;
    private Label categoryBreakdownLabel;
    private ProgressIndicator loadingIndicator;

    public HistoryView() {
        buildUI();
        loadCategories();
        loadHistoryDataAsync(null);
    }

    public Region getView() {
        return root;
    }

    private void buildUI() {
        root.setPadding(new Insets(24, 40, 30, 40));

        // Top Bar
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Admin Dashboard");
        backBtn.getStyleClass().addAll("button", "button-outline");
        backBtn.setOnAction(e -> NavigationManager.getInstance().showAdminDashboard());

        VBox titleBox = new VBox(2);
        Label title = new Label("Quiz Attempt History & Analytics");
        title.getStyleClass().add("title-medium");
        Label subtitle = new Label("Review performance records stored in local SQLite database.");
        subtitle.getStyleClass().add("subtitle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearBtn = new Button("Clear History");
        clearBtn.getStyleClass().addAll("button", "button-danger");
        clearBtn.setOnAction(e -> handleClearHistory());

        topBox.getChildren().addAll(backBtn, titleBox, spacer, clearBtn);
        root.setTop(topBox);

        // Center: Stats Cards + Filter + TableView
        VBox centerBox = new VBox(20);
        centerBox.setPadding(new Insets(20, 0, 0, 0));

        // Stats Panel (3 KPI Cards)
        HBox statsRow = new HBox(16);
        statsRow.setAlignment(Pos.CENTER);

        totalAttemptsLabel = new Label("0");
        bestScoreLabel = new Label("0.0%");
        avgScoreLabel = new Label("0.0%");

        VBox card1 = createStatCard("Total Attempts", totalAttemptsLabel, "#6366f1");
        VBox card2 = createStatCard("Highest Score", bestScoreLabel, "#10b981");
        VBox card3 = createStatCard("Average Score", avgScoreLabel, "#38bdf8");

        statsRow.getChildren().addAll(card1, card2, card3);
        HBox.setHgrow(card1, Priority.ALWAYS);
        HBox.setHgrow(card2, Priority.ALWAYS);
        HBox.setHgrow(card3, Priority.ALWAYS);

        // Filter Bar
        HBox filterBar = new HBox(14);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        Label filterLbl = new Label("Filter by Category:");
        filterLbl.getStyleClass().add("label-field");

        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.setPrefWidth(260);
        categoryFilterCombo.setOnAction(e -> {
            CategoryFilterItem selected = categoryFilterCombo.getValue();
            Integer catId = (selected != null && selected.id > 0) ? selected.id : null;
            loadHistoryDataAsync(catId);
        });

        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setPrefSize(22, 22);
        loadingIndicator.setVisible(false);

        categoryBreakdownLabel = new Label();
        categoryBreakdownLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        filterBar.getChildren().addAll(filterLbl, categoryFilterCombo, loadingIndicator, categoryBreakdownLabel);

        // TableView setup
        tableView = new TableView<>(attemptsList);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Attempt, Integer> colId = new TableColumn<>("#");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setMaxWidth(50);
        colId.setMinWidth(40);

        TableColumn<Attempt, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(new PropertyValueFactory<>("takenAt"));
        colDate.setMinWidth(150);

        TableColumn<Attempt, String> colUser = new TableColumn<>("Player Name");
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colUser.setMinWidth(120);

        TableColumn<Attempt, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colCat.setMinWidth(150);

        TableColumn<Attempt, String> colScore = new TableColumn<>("Score");
        colScore.setCellValueFactory(new PropertyValueFactory<>("formattedScore"));
        colScore.setMinWidth(80);

        TableColumn<Attempt, String> colPct = new TableColumn<>("Percentage");
        colPct.setCellValueFactory(new PropertyValueFactory<>("formattedPercentage"));
        colPct.setMinWidth(90);

        TableColumn<Attempt, String> colTime = new TableColumn<>("Time Taken");
        colTime.setCellValueFactory(new PropertyValueFactory<>("formattedTime"));
        colTime.setMinWidth(90);

        tableView.getColumns().addAll(colId, colDate, colUser, colCat, colScore, colPct, colTime);

        // Double click row to view review of past attempt
        tableView.setRowFactory(tv -> {
            TableRow<Attempt> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Attempt rowData = row.getItem();
                    NavigationManager.getInstance().showReviewScreen(null, rowData);
                }
            });
            return row;
        });

        VBox.setVgrow(tableView, Priority.ALWAYS);

        centerBox.getChildren().addAll(statsRow, filterBar, tableView);
        root.setCenter(centerBox);

        // Bottom Bar
        HBox bottomBox = new HBox(16);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(14, 0, 0, 0));

        Label hint = new Label("Tip: Double-click any row to inspect per-question answers.");
        hint.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);

        Button reviewSelectedBtn = new Button("Review Selected Attempt");
        reviewSelectedBtn.getStyleClass().addAll("button", "button-primary");
        reviewSelectedBtn.setOnAction(e -> {
            Attempt selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                NavigationManager.getInstance().showReviewScreen(null, selected);
            } else {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an attempt from the table to review.");
            }
        });

        bottomBox.getChildren().addAll(hint, bSpacer, reviewSelectedBtn);
        root.setBottom(bottomBox);
    }

    private VBox createStatCard(String title, Label valueLabel, String hexColor) {
        VBox card = new VBox(4);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("card-subtle");
        card.setPadding(new Insets(14));

        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-text-fill: " + hexColor + ";");

        Label tLbl = new Label(title);
        tLbl.getStyleClass().add("subtitle");

        card.getChildren().addAll(valueLabel, tLbl);
        return card;
    }

    private void loadCategories() {
        categoryFilterCombo.getItems().clear();
        categoryFilterCombo.getItems().add(new CategoryFilterItem(0, "All Categories"));
        for (Category c : categoryDAO.getAllCategories()) {
            categoryFilterCombo.getItems().add(new CategoryFilterItem(c.getId(), c.getName()));
        }
        categoryFilterCombo.getSelectionModel().selectFirst();
    }

    private void loadHistoryDataAsync(Integer categoryId) {
        loadingIndicator.setVisible(true);

        Task<HistoryDataPayload> task = new Task<>() {
            @Override
            protected HistoryDataPayload call() {
                List<Attempt> list = attemptDAO.getAttempts(categoryId);
                int totalCount = attemptDAO.getTotalAttemptsCount();
                double avgPct = attemptDAO.getAverageScorePercentage();
                double bestPct = attemptDAO.getBestScorePercentage();
                Map<String, Integer> perCat = attemptDAO.getAttemptsPerCategory();

                return new HistoryDataPayload(list, totalCount, avgPct, bestPct, perCat);
            }
        };

        task.setOnSucceeded(e -> {
            HistoryDataPayload result = task.getValue();
            attemptsList.clear();
            attemptsList.addAll(result.attempts);

            totalAttemptsLabel.setText(String.valueOf(result.totalCount));
            bestScoreLabel.setText(String.format("%.1f%%", result.bestPercentage));
            avgScoreLabel.setText(String.format("%.1f%%", result.averagePercentage));

            StringBuilder sb = new StringBuilder("Distribution: ");
            result.attemptsPerCategory.forEach((cat, cnt) -> sb.append(cat).append(" (").append(cnt).append(")  "));
            categoryBreakdownLabel.setText(sb.toString());

            loadingIndicator.setVisible(false);
        });

        task.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            System.err.println("Failed to load history: " + task.getException().getMessage());
        });

        dbExecutor.submit(task);
    }

    private void handleClearHistory() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear History");
        alert.setHeaderText("Delete all quiz attempts?");
        alert.setContentText("This action cannot be undone. All past exam records will be permanently removed.");

        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                if (attemptDAO.clearAllHistory()) {
                    loadHistoryDataAsync(null);
                }
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

    private static class CategoryFilterItem {
        final int id;
        final String name;

        CategoryFilterItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private record HistoryDataPayload(
            List<Attempt> attempts,
            int totalCount,
            double averagePercentage,
            double bestPercentage,
            Map<String, Integer> attemptsPerCategory
    ) {}
}
