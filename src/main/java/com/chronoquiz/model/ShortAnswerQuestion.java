package com.chronoquiz.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a short-answer question evaluated with case-insensitive and flexible keyword matching.
 * Demonstrates polymorphism by overriding checkAnswer().
 */
public class ShortAnswerQuestion extends Question {
    private List<String> acceptedAnswers = new ArrayList<>();

    public ShortAnswerQuestion() {
        setType(TYPE_SHORT_ANSWER);
    }

    public ShortAnswerQuestion(int id, int categoryId, String categoryName, String text, Difficulty difficulty, String correctAnswer, List<String> acceptedAnswers) {
        super(id, categoryId, categoryName, TYPE_SHORT_ANSWER, text, difficulty, correctAnswer);
        if (acceptedAnswers != null) {
            this.acceptedAnswers = new ArrayList<>(acceptedAnswers);
        }
        if (correctAnswer != null && !this.acceptedAnswers.contains(correctAnswer)) {
            this.acceptedAnswers.add(correctAnswer);
        }
    }

    public List<String> getAcceptedAnswers() {
        return acceptedAnswers;
    }

    public void setAcceptedAnswers(List<String> acceptedAnswers) {
        this.acceptedAnswers = acceptedAnswers != null ? acceptedAnswers : new ArrayList<>();
    }

    public void setAcceptedAnswersFromCsv(String csv) {
        this.acceptedAnswers.clear();
        if (csv != null && !csv.trim().isEmpty()) {
            String[] parts = csv.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    this.acceptedAnswers.add(trimmed);
                }
            }
        }
    }

    public String getAcceptedAnswersAsCsv() {
        return String.join(", ", acceptedAnswers);
    }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return false;
        }

        String normalizedInput = normalize(answer);

        // Check primary correct answer
        if (getCorrectAnswer() != null && normalize(getCorrectAnswer()).equals(normalizedInput)) {
            return true;
        }

        // Check all accepted alternative answers
        for (String accepted : acceptedAnswers) {
            if (normalize(accepted).equals(normalizedInput)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Helper to clean punctuation, extra spaces, and casing.
     */
    private String normalize(String s) {
        return s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    @Override
    public String getCorrectAnswerDisplay() {
        if (acceptedAnswers.isEmpty()) {
            return getCorrectAnswer() != null ? getCorrectAnswer() : "N/A";
        }
        return String.join(" OR ", acceptedAnswers);
    }
}
