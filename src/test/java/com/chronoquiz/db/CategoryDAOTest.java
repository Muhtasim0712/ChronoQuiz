package com.chronoquiz.db;

import com.chronoquiz.model.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryDAOTest {
    private CategoryDAO categoryDAO;

    @BeforeEach
    public void setUp() {
        DatabaseManager.getInstance();
        categoryDAO = new CategoryDAO();
    }

    @Test
    public void testCreateAndRetrieveCategory() {
        String testCatName = "Robotics & Automation " + System.currentTimeMillis();
        String testDesc = "Sensors, actuators, kinematics, and ROS2 frameworks.";

        int id = categoryDAO.createCategory(testCatName, testDesc);
        assertTrue(id > 0, "Created category ID should be greater than 0");

        try {
            Category cat = categoryDAO.getCategoryById(id);
            assertNotNull(cat);
            assertEquals(testCatName, cat.getName());
            assertEquals(testDesc, cat.getDescription());

            Category byName = categoryDAO.getCategoryByName(testCatName);
            assertNotNull(byName);
            assertEquals(id, byName.getId());

            assertTrue(categoryDAO.categoryExists(testCatName));
        } finally {
            categoryDAO.deleteCategory(id);
        }
    }

    @Test
    public void testDuplicateCategoryPrevention() {
        String uniqueName = "Cybersecurity " + System.currentTimeMillis();
        int id = categoryDAO.createCategory(uniqueName, "Network security, cryptography, and penetration testing.");
        assertTrue(id > 0);

        try {
            // Attempting to create category with same name (even different case) should return -1
            int duplicateId = categoryDAO.createCategory(uniqueName.toLowerCase(), "Duplicate attempt");
            assertEquals(-1, duplicateId);
        } finally {
            categoryDAO.deleteCategory(id);
        }
    }

    @Test
    public void testUpdateCategory() {
        String originalName = "Bioinformatics " + System.currentTimeMillis();
        int id = categoryDAO.createCategory(originalName, "Genomics and protein sequence alignment.");
        assertTrue(id > 0);

        try {
            String updatedName = "Computational Biology " + System.currentTimeMillis();
            String updatedDesc = "Structural biology, RNA sequencing, and machine learning in genomics.";

            boolean success = categoryDAO.updateCategory(id, updatedName, updatedDesc);
            assertTrue(success);

            Category updated = categoryDAO.getCategoryById(id);
            assertNotNull(updated);
            assertEquals(updatedName, updated.getName());
            assertEquals(updatedDesc, updated.getDescription());
        } finally {
            categoryDAO.deleteCategory(id);
        }
    }

    @Test
    public void testGetAllCategoriesContainsDescription() {
        String name = "Quantum Computing " + System.currentTimeMillis();
        String desc = "Qubits, quantum gates, entanglement, and Shor's algorithm.";
        int id = categoryDAO.createCategory(name, desc);
        assertTrue(id > 0);

        try {
            List<Category> all = categoryDAO.getAllCategories();
            assertFalse(all.isEmpty());

            boolean found = false;
            for (Category c : all) {
                if (c.getId() == id) {
                    assertEquals(name, c.getName());
                    assertEquals(desc, c.getDescription());
                    found = true;
                    break;
                }
            }
            assertTrue(found, "Newly created category should be in getAllCategories()");
        } finally {
            categoryDAO.deleteCategory(id);
        }
    }
}
