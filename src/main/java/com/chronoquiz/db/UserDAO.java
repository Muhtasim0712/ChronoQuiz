package com.chronoquiz.db;

import com.chronoquiz.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for users/players in SQLite.
 */
public class UserDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public User getUserById(int id) {
        String sql = "SELECT id, name, created_at FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("name"), rs.getString("created_at"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
        }
        return null;
    }

    public User getUserByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT id, name, created_at FROM users WHERE LOWER(name) = LOWER(?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new User(rs.getInt("id"), rs.getString("name"), rs.getString("created_at"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by name: " + e.getMessage());
        }
        return null;
    }

    public User getOrCreateUser(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "Anonymous";
        }
        name = name.trim();

        User existing = getUserByName(name);
        if (existing != null) {
            return existing;
        }

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String insertSql = "INSERT INTO users (name, created_at) VALUES (?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, now);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new User(rs.getInt(1), name, now);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting user: " + e.getMessage());
        }
        return new User(1, name, now);
    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, created_at FROM users ORDER BY name ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(new User(rs.getInt("id"), rs.getString("name"), rs.getString("created_at")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }
        return users;
    }
}
