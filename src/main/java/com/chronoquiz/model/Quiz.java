package com.chronoquiz.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordinates an active quiz session, questions, timer boundary, and scoring.
 */
public class Quiz {
    private String userName;
    private String categoryName;
    private int categoryId;
    private Difficulty difficulty;
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private int totalTimeSeconds = 60;
    private int timeRemainingSeconds = 60;
    private boolean submitted = false;

    public Quiz(String userName, String categoryName, int categoryId, Difficulty difficulty, List<Question> questions, int totalTimeSeconds) {
        this.userName = (userName == null || userName.trim().isEmpty()) ? "Anonymous" : userName.trim();
        this.categoryName = categoryName != null ? categoryName : "General";
        this.categoryId = categoryId;
        this.difficulty = difficulty != null ? difficulty : Difficulty.MEDIUM;
        this.questions = (questions != null) ? new ArrayList<>(questions) : new ArrayList<>();
        this.totalTimeSeconds = Math.max(10, totalTimeSeconds);
        this.timeRemainingSeconds = this.totalTimeSeconds;
    }

    public boolean hasNext() {
        return currentIndex < questions.size() - 1;
    }

    public Question getCurrentQuestion() {
        if (currentIndex >= 0 && currentIndex < questions.size()) {
            return questions.get(currentIndex);
        }
        return null;
    }

    public boolean nextQuestion() {
        if (hasNext()) {
            currentIndex++;
            return true;
        }
        return false;
    }

    public void submit() {
        this.submitted = true;
    }

    public int calculateScore() {
        int score = 0;
        for (Question q : questions) {
            if (q.isCorrect()) {
                score++;
            }
        }
        return score;
    }

    public int getCorrectCount() {
        return calculateScore();
    }

    public int getIncorrectCount() {
        return questions.size() - getCorrectCount();
    }

    public double getPercentage() {
        if (questions.isEmpty()) return 0.0;
        return ((double) getCorrectCount() / questions.size()) * 100.0;
    }

    public int getTimeTakenSeconds() {
        return totalTimeSeconds - timeRemainingSeconds;
    }

    // Getters and Setters
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public int getTotalTimeSeconds() {
        return totalTimeSeconds;
    }

    public int getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public void setTimeRemainingSeconds(int timeRemainingSeconds) {
        this.timeRemainingSeconds = Math.max(0, timeRemainingSeconds);
    }

    public boolean isSubmitted() {
        return submitted;
    }
}
