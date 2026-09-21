package com.chronoquiz.model;

/**
 * Abstract base class representing a quiz question.
 * Demonstrates OOP concepts: inheritance, abstraction, and encapsulation.
 */
public abstract class Question {
    public static final String TYPE_MULTIPLE_CHOICE = "MULTIPLE_CHOICE";
    public static final String TYPE_SHORT_ANSWER = "SHORT_ANSWER";

    private int id;
    private int categoryId;
    private String categoryName;
    private String type;
    private String text;
    private Difficulty difficulty = Difficulty.MEDIUM;
    private String correctAnswer;
    private String userAnswer;

    public Question() {
    }

    public Question(int id, int categoryId, String categoryName, String type, String text, Difficulty difficulty, String correctAnswer) {
        this.id = id;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.type = type;
        this.text = text;
        this.difficulty = difficulty != null ? difficulty : Difficulty.MEDIUM;
        this.correctAnswer = correctAnswer;
    }

    /**
     * Checks whether the user's provided answer is correct.
     * Polymorphic method implemented differently by subclasses.
     */
    public abstract boolean checkAnswer(String answer);

    /**
     * Checks whether the current user answer is correct.
     */
    public boolean isCorrect() {
        return checkAnswer(userAnswer);
    }

    /**
     * Returns true if user has submitted or selected an answer.
     */
    public boolean isAnswered() {
        return userAnswer != null && !userAnswer.trim().isEmpty();
    }

    /**
     * Gets a readable representation of the correct answer for the review screen.
     */
    public abstract String getCorrectAnswerDisplay();

    // Getters and Setters (Encapsulation)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    @Override
    public String toString() {
        return text;
    }
}
