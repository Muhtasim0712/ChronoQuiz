package com.chronoquiz.db;

import com.chronoquiz.model.Difficulty;
import com.chronoquiz.model.MultipleChoiceQuestion;
import com.chronoquiz.model.Option;
import com.chronoquiz.model.ShortAnswerQuestion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Manages SQLite database connection, table initialization, and initial seed data.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:chronoquiz.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
            instance.initializeDatabase();
        }
        return instance;
    }

    /**
     * Gets a connection to the SQLite database with foreign keys enabled.
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Creates required database tables if they do not already exist.
     */
    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    created_at TEXT NOT NULL
                );
            """);

            // 2. Categories table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                );
            """);

            // 3. Questions table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS questions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    category_id INTEGER,
                    type TEXT NOT NULL,
                    text TEXT NOT NULL,
                    difficulty TEXT NOT NULL,
                    correct_answer TEXT NOT NULL,
                    accepted_answers TEXT,
                    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
                );
            """);

            // 4. Options table (for multiple-choice)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS options (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    question_id INTEGER NOT NULL,
                    option_text TEXT NOT NULL,
                    is_correct INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
                );
            """);

            // 5. Attempts table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attempts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    category_id INTEGER,
                    score INTEGER NOT NULL,
                    total INTEGER NOT NULL,
                    time_taken_sec INTEGER NOT NULL,
                    taken_at TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
                );
            """);

            // 6. Attempt answers table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attempt_answers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    attempt_id INTEGER NOT NULL,
                    question_id INTEGER,
                    user_answer TEXT,
                    is_correct INTEGER NOT NULL,
                    FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE,
                    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE SET NULL
                );
            """);

            // 7. Settings table (for admin password and active exam configuration)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );
            """);

            // Seed default settings and questions if database is fresh
            seedDefaultSettings();
            seedDefaultData();

        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void seedDefaultSettings() {
        String insertSql = "INSERT OR IGNORE INTO settings (key, value) VALUES (?, ?)";
        try (Connection conn = getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(insertSql)) {
            String[][] defaults = {
                    {"admin_password", "admin123"},
                    {"quiz_time_limit_sec", "60"},
                    {"quiz_question_count", "5"},
                    {"quiz_category_id", "0"},
                    {"quiz_category_name", "All Categories (Mixed)"},
                    {"quiz_difficulty", "ANY"},
                    {"quiz_source", "LOCAL"}
            };
            for (String[] def : defaults) {
                stmt.setString(1, def[0]);
                stmt.setString(2, def[1]);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Note during seed settings: " + e.getMessage());
        }
    }

    /**
     * Seeds initial categories and diverse questions if the question bank is empty.
     */
    private void seedDefaultData() {
        CategoryDAO categoryDAO = new CategoryDAO();
        QuestionDAO questionDAO = new QuestionDAO();

        try {
            if (questionDAO.getQuestionCount() > 0) {
                return; // Database already seeded
            }

            int catCS = categoryDAO.getOrCreateCategory("Computer Science");
            int catScience = categoryDAO.getOrCreateCategory("Science & Nature");
            int catHistory = categoryDAO.getOrCreateCategory("History");
            int catGeneral = categoryDAO.getOrCreateCategory("General Knowledge");

            // --- Computer Science Questions ---
            MultipleChoiceQuestion mcq1 = new MultipleChoiceQuestion(
                    0, catCS, "Computer Science",
                    "Which data structure operates on a Last-In, First-Out (LIFO) principle?",
                    Difficulty.EASY, "Stack",
                    Arrays.asList(
                            new Option("Stack", true),
                            new Option("Queue", false),
                            new Option("Array", false),
                            new Option("Linked List", false)
                    )
            );
            questionDAO.insertQuestion(mcq1);

            MultipleChoiceQuestion mcq2 = new MultipleChoiceQuestion(
                    0, catCS, "Computer Science",
                    "What is the average time complexity of searching in a balanced Binary Search Tree?",
                    Difficulty.MEDIUM, "O(log n)",
                    Arrays.asList(
                            new Option("O(log n)", true),
                            new Option("O(n)", false),
                            new Option("O(1)", false),
                            new Option("O(n log n)", false)
                    )
            );
            questionDAO.insertQuestion(mcq2);

            ShortAnswerQuestion saq1 = new ShortAnswerQuestion(
                    0, catCS, "Computer Science",
                    "What keyword is used in Java to inherit from another class?",
                    Difficulty.EASY, "extends",
                    Arrays.asList("extends")
            );
            questionDAO.insertQuestion(saq1);

            ShortAnswerQuestion saq2 = new ShortAnswerQuestion(
                    0, catCS, "Computer Science",
                    "Which Java interface must be implemented to create a task executable by a Thread?",
                    Difficulty.MEDIUM, "Runnable",
                    Arrays.asList("Runnable", "Callable")
            );
            questionDAO.insertQuestion(saq2);

            // --- Science Questions ---
            MultipleChoiceQuestion mcq3 = new MultipleChoiceQuestion(
                    0, catScience, "Science & Nature",
                    "What chemical element has the symbol 'Fe'?",
                    Difficulty.EASY, "Iron",
                    Arrays.asList(
                            new Option("Iron", true),
                            new Option("Fluorine", false),
                            new Option("Francium", false),
                            new Option("Lead", false)
                    )
            );
            questionDAO.insertQuestion(mcq3);

            ShortAnswerQuestion saq3 = new ShortAnswerQuestion(
                    0, catScience, "Science & Nature",
                    "What organelle is known as the powerhouse of the eukaryotic cell?",
                    Difficulty.MEDIUM, "Mitochondria",
                    Arrays.asList("Mitochondria", "Mitochondrion")
            );
            questionDAO.insertQuestion(saq3);

            // --- History Questions ---
            MultipleChoiceQuestion mcq4 = new MultipleChoiceQuestion(
                    0, catHistory, "History",
                    "In which year did World War II officially end?",
                    Difficulty.MEDIUM, "1945",
                    Arrays.asList(
                            new Option("1945", true),
                            new Option("1939", false),
                            new Option("1941", false),
                            new Option("1950", false)
                    )
            );
            questionDAO.insertQuestion(mcq4);

            ShortAnswerQuestion saq4 = new ShortAnswerQuestion(
                    0, catHistory, "History",
                    "Who was the first President of the United States?",
                    Difficulty.EASY, "George Washington",
                    Arrays.asList("George Washington", "Washington")
            );
            questionDAO.insertQuestion(saq4);

            // --- General Knowledge Questions ---
            MultipleChoiceQuestion mcq5 = new MultipleChoiceQuestion(
                    0, catGeneral, "General Knowledge",
                    "How many continents are there on Earth?",
                    Difficulty.EASY, "7",
                    Arrays.asList(
                            new Option("7", true),
                            new Option("5", false),
                            new Option("6", false),
                            new Option("8", false)
                    )
            );
            questionDAO.insertQuestion(mcq5);

            ShortAnswerQuestion saq5 = new ShortAnswerQuestion(
                    0, catGeneral, "General Knowledge",
                    "What is the capital city of France?",
                    Difficulty.EASY, "Paris",
                    Arrays.asList("Paris")
            );
            questionDAO.insertQuestion(saq5);

        } catch (Exception e) {
            System.err.println("Note during seed data: " + e.getMessage());
        }
    }
}
