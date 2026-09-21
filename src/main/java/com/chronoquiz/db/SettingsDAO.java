package com.chronoquiz.db;

import com.chronoquiz.model.Difficulty;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data Access Object for application and exam settings in SQLite.
 * Manages admin password and active exam parameters.
 */
public class SettingsDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public String getSetting(String key, String defaultValue) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching setting '" + key + "': " + e.getMessage());
        }
        return defaultValue;
    }

    public void setSetting(String key, String value) {
        String sql = "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, key);
            stmt.setString(2, value);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving setting '" + key + "': " + e.getMessage());
        }
    }

    public String getAdminPassword() {
        return getSetting("admin_password", "admin123");
    }

    public void setAdminPassword(String password) {
        setSetting("admin_password", password);
    }

    public int getQuizTimeLimitSeconds() {
        try {
            return Integer.parseInt(getSetting("quiz_time_limit_sec", "60"));
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    public void setQuizTimeLimitSeconds(int seconds) {
        setSetting("quiz_time_limit_sec", String.valueOf(Math.max(10, seconds)));
    }

    public int getQuizQuestionCount() {
        try {
            return Integer.parseInt(getSetting("quiz_question_count", "5"));
        } catch (NumberFormatException e) {
            return 5;
        }
    }

    public void setQuizQuestionCount(int count) {
        setSetting("quiz_question_count", String.valueOf(Math.max(1, count)));
    }

    public int getQuizCategoryId() {
        try {
            return Integer.parseInt(getSetting("quiz_category_id", "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public String getQuizCategoryName() {
        return getSetting("quiz_category_name", "All Categories (Mixed)");
    }

    public void setQuizCategory(int id, String name) {
        setSetting("quiz_category_id", String.valueOf(id));
        setSetting("quiz_category_name", name != null ? name : "All Categories (Mixed)");
    }

    public Difficulty getQuizDifficulty() {
        return Difficulty.fromString(getSetting("quiz_difficulty", "ANY"));
    }

    public void setQuizDifficulty(Difficulty difficulty) {
        setSetting("quiz_difficulty", difficulty != null ? difficulty.name() : "ANY");
    }

    public String getQuizSource() {
        return getSetting("quiz_source", "LOCAL");
    }

    public void setQuizSource(String source) {
        setSetting("quiz_source", source != null ? source : "LOCAL");
    }
}
