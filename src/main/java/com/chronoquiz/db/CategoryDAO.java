package com.chronoquiz.db;

import com.chronoquiz.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for categories in SQLite.
 */
public class CategoryDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name FROM categories ORDER BY name ASC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
        }
        return categories;
    }

    public Category getCategoryById(int id) {
        String sql = "SELECT id, name FROM categories WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Category(rs.getInt("id"), rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching category by ID: " + e.getMessage());
        }
        return null;
    }

    public Category getCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT id, name FROM categories WHERE LOWER(name) = LOWER(?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Category(rs.getInt("id"), rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching category by name: " + e.getMessage());
        }
        return null;
    }

    public int getOrCreateCategory(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "General Knowledge";
        }
        name = name.trim();

        Category existing = getCategoryByName(name);
        if (existing != null) {
            return existing.getId();
        }

        String insertSql = "INSERT INTO categories (name) VALUES (?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting category: " + e.getMessage());
        }
        return 1;
    }
}
