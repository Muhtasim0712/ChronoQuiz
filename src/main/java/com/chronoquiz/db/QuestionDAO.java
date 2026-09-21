package com.chronoquiz.db;

import com.chronoquiz.model.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data Access Object for questions and options in SQLite.
 * Demonstrates PreparedStatement, try-with-resources, transactions, and polymorphism.
 */
public class QuestionDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public int getQuestionCount() {
        String sql = "SELECT COUNT(*) FROM questions";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting question count: " + e.getMessage());
        }
        return 0;
    }

    public boolean questionExists(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String sql = "SELECT id FROM questions WHERE LOWER(TRIM(text)) = LOWER(TRIM(?))";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, text);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking question existence: " + e.getMessage());
        }
        return false;
    }

    public List<Question> getAllQuestions() {
        List<Question> list = new ArrayList<>();
        String sql = """
            SELECT q.id, q.category_id, c.name AS category_name, q.type, q.text, q.difficulty, q.correct_answer, q.accepted_answers
            FROM questions q
            LEFT JOIN categories c ON q.category_id = c.id
            ORDER BY q.id DESC
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Question q = mapRowToQuestion(conn, rs);
                if (q != null) {
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all questions: " + e.getMessage());
        }
        return list;
    }

    public List<Question> getQuestions(Integer categoryId, Difficulty difficulty, int limit) {
        List<Question> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT q.id, q.category_id, c.name AS category_name, q.type, q.text, q.difficulty, q.correct_answer, q.accepted_answers
            FROM questions q
            LEFT JOIN categories c ON q.category_id = c.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND q.category_id = ?");
            params.add(categoryId);
        }

        if (difficulty != null && difficulty != Difficulty.ANY) {
            sql.append(" AND q.difficulty = ?");
            params.add(difficulty.name());
        }

        sql.append(" ORDER BY RANDOM()");
        if (limit > 0) {
            sql.append(" LIMIT ?");
            params.add(limit);
        }

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Question q = mapRowToQuestion(conn, rs);
                    if (q != null) {
                        if (q instanceof MultipleChoiceQuestion mcq) {
                            mcq.shuffleOptions();
                        }
                        list.add(q);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching quiz questions: " + e.getMessage());
        }
        return list;
    }

    public boolean insertQuestion(Question question) {
        String insertQuestionSql = """
            INSERT INTO questions (category_id, type, text, difficulty, correct_answer, accepted_answers)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Transaction

            try (PreparedStatement stmt = conn.prepareStatement(insertQuestionSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, question.getCategoryId());
                stmt.setString(2, question.getType());
                stmt.setString(3, question.getText());
                stmt.setString(4, question.getDifficulty().name());
                stmt.setString(5, question.getCorrectAnswer());

                if (question instanceof ShortAnswerQuestion saq) {
                    stmt.setString(6, saq.getAcceptedAnswersAsCsv());
                } else {
                    stmt.setNull(6, Types.VARCHAR);
                }

                stmt.executeUpdate();

                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int qId = generatedKeys.getInt(1);
                        question.setId(qId);

                        // If multiple choice, insert options
                        if (question instanceof MultipleChoiceQuestion mcq) {
                            String insertOptionSql = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?)";
                            try (PreparedStatement optStmt = conn.prepareStatement(insertOptionSql)) {
                                for (Option opt : mcq.getOptions()) {
                                    optStmt.setInt(1, qId);
                                    optStmt.setString(2, opt.getOptionText());
                                    optStmt.setInt(3, opt.isCorrect() ? 1 : 0);
                                    optStmt.addBatch();
                                }
                                optStmt.executeBatch();
                            }
                        }
                    }
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error inserting question (rolling back): " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean updateQuestion(Question question) {
        String updateSql = """
            UPDATE questions
            SET category_id = ?, type = ?, text = ?, difficulty = ?, correct_answer = ?, accepted_answers = ?
            WHERE id = ?
        """;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Transaction

            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setInt(1, question.getCategoryId());
                stmt.setString(2, question.getType());
                stmt.setString(3, question.getText());
                stmt.setString(4, question.getDifficulty().name());
                stmt.setString(5, question.getCorrectAnswer());

                if (question instanceof ShortAnswerQuestion saq) {
                    stmt.setString(6, saq.getAcceptedAnswersAsCsv());
                } else {
                    stmt.setNull(6, Types.VARCHAR);
                }
                stmt.setInt(7, question.getId());
                stmt.executeUpdate();
            }

            // If multiple choice, refresh options
            if (question instanceof MultipleChoiceQuestion mcq) {
                try (PreparedStatement delStmt = conn.prepareStatement("DELETE FROM options WHERE question_id = ?")) {
                    delStmt.setInt(1, question.getId());
                    delStmt.executeUpdate();
                }

                String insertOptionSql = "INSERT INTO options (question_id, option_text, is_correct) VALUES (?, ?, ?)";
                try (PreparedStatement optStmt = conn.prepareStatement(insertOptionSql)) {
                    for (Option opt : mcq.getOptions()) {
                        optStmt.setInt(1, question.getId());
                        optStmt.setString(2, opt.getOptionText());
                        optStmt.setInt(3, opt.isCorrect() ? 1 : 0);
                        optStmt.addBatch();
                    }
                    optStmt.executeBatch();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating question (rolling back): " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean deleteQuestion(int questionId) {
        String sql = "DELETE FROM questions WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, questionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting question: " + e.getMessage());
            return false;
        }
    }

    public List<Option> getOptionsForQuestion(Connection conn, int questionId) throws SQLException {
        List<Option> options = new ArrayList<>();
        String sql = "SELECT id, question_id, option_text, is_correct FROM options WHERE question_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    options.add(new Option(
                            rs.getInt("id"),
                            rs.getInt("question_id"),
                            rs.getString("option_text"),
                            rs.getInt("is_correct") == 1
                    ));
                }
            }
        }
        return options;
    }

    private Question mapRowToQuestion(Connection conn, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int categoryId = rs.getInt("category_id");
        String categoryName = rs.getString("category_name");
        String type = rs.getString("type");
        String text = rs.getString("text");
        Difficulty difficulty = Difficulty.fromString(rs.getString("difficulty"));
        String correctAnswer = rs.getString("correct_answer");
        String acceptedAnswers = rs.getString("accepted_answers");

        if (Question.TYPE_SHORT_ANSWER.equalsIgnoreCase(type)) {
            ShortAnswerQuestion saq = new ShortAnswerQuestion(id, categoryId, categoryName, text, difficulty, correctAnswer, null);
            saq.setAcceptedAnswersFromCsv(acceptedAnswers);
            return saq;
        } else {
            List<Option> options = getOptionsForQuestion(conn, id);
            return new MultipleChoiceQuestion(id, categoryId, categoryName, text, difficulty, correctAnswer, options);
        }
    }
}
