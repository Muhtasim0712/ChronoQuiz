package com.chronoquiz.db;

import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.AttemptAnswer;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object for quiz attempts and attempt answers.
 * Implements strict database transactions (commit/rollback) and aggregate reporting.
 */
public class AttemptDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public boolean saveAttempt(Attempt attempt) {
        String insertAttemptSql = """
            INSERT INTO attempts (user_id, category_id, score, total, time_taken_sec, taken_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        String insertAnswerSql = """
            INSERT INTO attempt_answers (attempt_id, question_id, user_answer, is_correct)
            VALUES (?, ?, ?, ?)
        """;

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            int attemptId;
            try (PreparedStatement stmt = conn.prepareStatement(insertAttemptSql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, attempt.getUserId());
                if (attempt.getCategoryId() > 0) {
                    stmt.setInt(2, attempt.getCategoryId());
                } else {
                    stmt.setNull(2, Types.INTEGER);
                }
                stmt.setInt(3, attempt.getScore());
                stmt.setInt(4, attempt.getTotal());
                stmt.setInt(5, attempt.getTimeTakenSec());
                stmt.setString(6, attempt.getTakenAt());

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        attemptId = rs.getInt(1);
                        attempt.setId(attemptId);
                    } else {
                        throw new SQLException("Failed to obtain generated ID for attempt.");
                    }
                }
            }

            // Insert each attempt answer
            if (attempt.getAnswers() != null && !attempt.getAnswers().isEmpty()) {
                try (PreparedStatement ansStmt = conn.prepareStatement(insertAnswerSql)) {
                    for (AttemptAnswer ans : attempt.getAnswers()) {
                        ansStmt.setInt(1, attemptId);
                        if (ans.getQuestionId() > 0) {
                            ansStmt.setInt(2, ans.getQuestionId());
                        } else {
                            ansStmt.setNull(2, Types.INTEGER);
                        }
                        ansStmt.setString(3, ans.getUserAnswer());
                        ansStmt.setInt(4, ans.isCorrect() ? 1 : 0);
                        ansStmt.addBatch();
                    }
                    ansStmt.executeBatch();
                }
            }

            conn.commit(); // Commit transaction
            return true;
        } catch (SQLException e) {
            System.err.println("Transaction failed while saving attempt: " + e.getMessage());
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

    public List<Attempt> getAttempts(Integer categoryId) {
        List<Attempt> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
            SELECT a.id, a.user_id, u.name AS user_name, a.category_id,
                   COALESCE(c.name, 'Mixed / Online') AS category_name,
                   a.score, a.total, a.time_taken_sec, a.taken_at
            FROM attempts a
            JOIN users u ON a.user_id = u.id
            LEFT JOIN categories c ON a.category_id = c.id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND a.category_id = ?");
            params.add(categoryId);
        }
        sql.append(" ORDER BY a.id DESC");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Attempt(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("user_name"),
                            rs.getInt("category_id"),
                            rs.getString("category_name"),
                            rs.getInt("score"),
                            rs.getInt("total"),
                            rs.getInt("time_taken_sec"),
                            rs.getString("taken_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching attempts: " + e.getMessage());
        }
        return list;
    }

    public List<AttemptAnswer> getAnswersForAttempt(int attemptId) {
        List<AttemptAnswer> answers = new ArrayList<>();
        String sql = """
            SELECT aa.id, aa.attempt_id, aa.question_id,
                   COALESCE(q.text, 'Question ' || aa.question_id) AS question_text,
                   COALESCE(q.correct_answer, '') AS correct_answer,
                   aa.user_answer, aa.is_correct
            FROM attempt_answers aa
            LEFT JOIN questions q ON aa.question_id = q.id
            WHERE aa.attempt_id = ?
            ORDER BY aa.id ASC
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, attemptId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    answers.add(new AttemptAnswer(
                            rs.getInt("id"),
                            rs.getInt("attempt_id"),
                            rs.getInt("question_id"),
                            rs.getString("question_text"),
                            rs.getString("user_answer"),
                            rs.getString("correct_answer"),
                            rs.getInt("is_correct") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching answers for attempt: " + e.getMessage());
        }
        return answers;
    }

    public int getTotalAttemptsCount() {
        String sql = "SELECT COUNT(*) FROM attempts";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting total attempts: " + e.getMessage());
        }
        return 0;
    }

    public double getAverageScorePercentage() {
        String sql = "SELECT AVG((CAST(score AS REAL) / total) * 100) FROM attempts WHERE total > 0";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting avg percentage: " + e.getMessage());
        }
        return 0.0;
    }

    public double getBestScorePercentage() {
        String sql = "SELECT MAX((CAST(score AS REAL) / total) * 100) FROM attempts WHERE total > 0";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting best percentage: " + e.getMessage());
        }
        return 0.0;
    }

    public Map<String, Integer> getAttemptsPerCategory() {
        Map<String, Integer> map = new HashMap<>();
        String sql = """
            SELECT COALESCE(c.name, 'Mixed / Online') AS cat_name, COUNT(a.id) AS cnt
            FROM attempts a
            LEFT JOIN categories c ON a.category_id = c.id
            GROUP BY cat_name
            ORDER BY cnt DESC
        """;

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("cat_name"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting attempts per category: " + e.getMessage());
        }
        return map;
    }

    public boolean clearAllHistory() {
        String sql = "DELETE FROM attempts";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            System.err.println("Error clearing attempt history: " + e.getMessage());
            return false;
        }
    }
}
