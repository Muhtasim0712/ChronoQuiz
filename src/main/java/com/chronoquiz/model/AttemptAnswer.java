package com.chronoquiz.model;

/**
 * Represents the specific answer given for each question in a quiz attempt.
 */
public class AttemptAnswer {
    private int id;
    private int attemptId;
    private int questionId;
    private String questionText;
    private String userAnswer;
    private String correctAnswer;
    private boolean correct;

    public AttemptAnswer() {
    }

    public AttemptAnswer(int id, int attemptId, int questionId, String questionText, String userAnswer, String correctAnswer, boolean correct) {
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.questionText = questionText;
        this.userAnswer = userAnswer;
        this.correctAnswer = correctAnswer;
        this.correct = correct;
    }

    public AttemptAnswer(int questionId, String questionText, String userAnswer, String correctAnswer, boolean correct) {
        this(0, 0, questionId, questionText, userAnswer, correctAnswer, correct);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }
}
