package com.chronoquiz.db;

import com.chronoquiz.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for categories in SQLite.
 * Provides complete CRUD operations, usage statistics, and transaction-safe modifications.
 */
public class CategoryDAO {
    private final DatabaseManager dbManager = DatabaseManager.getInstance();

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = """
            SELECT c.id, c.name, COALESCE(c.description, '') AS description,
                   COUNT(DISTINCT q.id) AS question_count,
                   COUNT(DISTINCT a.id) AS attempt_count
            FROM categories c
            LEFT JOIN questions q ON c.id = q.category_id
            LEFT JOIN attempts a ON c.id = a.category_id
            GROUP BY c.id, c.name, c.description
            ORDER BY c.name ASC
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                categories.add(new Category(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getInt("question_count"),
                        rs.getInt("attempt_count")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching categories with stats: " + e.getMessage());
        }
        return categories;
    }

    public Category getCategoryById(int id) {
        String sql = """
            SELECT c.id, c.name, COALESCE(c.description, '') AS description,
                   COUNT(DISTINCT q.id) AS question_count,
                   COUNT(DISTINCT a.id) AS attempt_count
            FROM categories c
            LEFT JOIN questions q ON c.id = q.category_id
            LEFT JOIN attempts a ON c.id = a.category_id
            WHERE c.id = ?
            GROUP BY c.id, c.name, c.description
        """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Category(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getInt("question_count"),
                            rs.getInt("attempt_count")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching category by ID: " + e.getMessage());
        }
        return null;
    }

    public Category getCategoryByName(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = """
            SELECT c.id, c.name, COALESCE(c.description, '') AS description,
                   COUNT(DISTINCT q.id) AS question_count,
                   COUNT(DISTINCT a.id) AS attempt_count
            FROM categories c
            LEFT JOIN questions q ON c.id = q.category_id
            LEFT JOIN attempts a ON c.id = a.category_id
            WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(?))
            GROUP BY c.id, c.name, c.description
        """;

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Category(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getInt("question_count"),
                            rs.getInt("attempt_count")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching category by name: " + e.getMessage());
        }
        return null;
    }

    public boolean categoryExists(String name) {
        if (name == null || name.trim().isEmpty()) return false;
        String sql = "SELECT id FROM categories WHERE LOWER(TRIM(name)) = LOWER(TRIM(?))";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking category existence: " + e.getMessage());
        }
        return false;
    }

    public int createCategory(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            return -1;
        }
        name = name.trim();
        description = description != null ? description.trim() : "";

        if (categoryExists(name)) {
            return -1; // Duplicate name
        }

        String sql = "INSERT INTO categories (name, description) VALUES (?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting new category: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateCategory(int id, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        name = name.trim();
        description = description != null ? description.trim() : "";

        // Check if another category already has this name
        Category existing = getCategoryByName(name);
        if (existing != null && existing.getId() != id) {
            return false; // Name conflict
        }

        String sql = "UPDATE categories SET name = ?, description = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating category: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteCategory(int id) {
        String unlinkQuestions = "UPDATE questions SET category_id = NULL WHERE category_id = ?";
        String unlinkAttempts = "UPDATE attempts SET category_id = NULL WHERE category_id = ?";
        String deleteSql = "DELETE FROM categories WHERE id = ?";

        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement s1 = conn.prepareStatement(unlinkQuestions);
                 PreparedStatement s2 = conn.prepareStatement(unlinkAttempts);
                 PreparedStatement s3 = conn.prepareStatement(deleteSql)) {

                s1.setInt(1, id);
                s1.executeUpdate();

                s2.setInt(1, id);
                s2.executeUpdate();

                s3.setInt(1, id);
                int affected = s3.executeUpdate();

                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error deleting category: " + e.getMessage());
            return false;
        }
    }

    public int getCategoryCount() {
        String sql = "SELECT COUNT(*) FROM categories";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting category count: " + e.getMessage());
        }
        return 0;
    }

    public int getOrCreateCategory(String name) {
        return getOrCreateCategory(name, "");
    }

    public int getOrCreateCategory(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            name = "General Knowledge";
        }
        name = name.trim();

        Category existing = getCategoryByName(name);
        if (existing != null) {
            // If existing category has no description and we provided one, update it
            if ((existing.getDescription() == null || existing.getDescription().isEmpty())
                    && description != null && !description.trim().isEmpty()) {
                updateCategory(existing.getId(), existing.getName(), description);
            }
            return existing.getId();
        }

        return createCategory(name, description);
    }
}
