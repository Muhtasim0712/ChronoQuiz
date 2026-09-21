package com.chronoquiz.db;

import com.chronoquiz.model.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsDAOTest {
    private SettingsDAO settingsDAO;

    @BeforeEach
    public void setUp() {
        DatabaseManager.getInstance();
        settingsDAO = new SettingsDAO();
    }

    @Test
    public void testDefaultAdminPassword() {
        String pass = settingsDAO.getAdminPassword();
        assertNotNull(pass);
        assertFalse(pass.isEmpty());
    }

    @Test
    public void testUpdateAdminPassword() {
        String original = settingsDAO.getAdminPassword();
        try {
            settingsDAO.setAdminPassword("newAdminSecret99");
            assertEquals("newAdminSecret99", settingsDAO.getAdminPassword());
        } finally {
            settingsDAO.setAdminPassword(original);
        }
    }

    @Test
    public void testExamTimeLimit() {
        int original = settingsDAO.getQuizTimeLimitSeconds();
        try {
            settingsDAO.setQuizTimeLimitSeconds(90);
            assertEquals(90, settingsDAO.getQuizTimeLimitSeconds());

            // Should enforce minimum of 10s
            settingsDAO.setQuizTimeLimitSeconds(2);
            assertEquals(10, settingsDAO.getQuizTimeLimitSeconds());
        } finally {
            settingsDAO.setQuizTimeLimitSeconds(original);
        }
    }

    @Test
    public void testExamQuestionCount() {
        int original = settingsDAO.getQuizQuestionCount();
        try {
            settingsDAO.setQuizQuestionCount(15);
            assertEquals(15, settingsDAO.getQuizQuestionCount());
        } finally {
            settingsDAO.setQuizQuestionCount(original);
        }
    }

    @Test
    public void testExamDifficultyAndCategory() {
        Difficulty origDiff = settingsDAO.getQuizDifficulty();
        int origCatId = settingsDAO.getQuizCategoryId();
        String origCatName = settingsDAO.getQuizCategoryName();

        try {
            settingsDAO.setQuizDifficulty(Difficulty.HARD);
            assertEquals(Difficulty.HARD, settingsDAO.getQuizDifficulty());

            settingsDAO.setQuizCategory(2, "Science & Nature");
            assertEquals(2, settingsDAO.getQuizCategoryId());
            assertEquals("Science & Nature", settingsDAO.getQuizCategoryName());
        } finally {
            settingsDAO.setQuizDifficulty(origDiff);
            settingsDAO.setQuizCategory(origCatId, origCatName);
        }
    }
}
