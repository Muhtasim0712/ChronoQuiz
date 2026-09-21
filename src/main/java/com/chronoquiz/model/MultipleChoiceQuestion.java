package com.chronoquiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a multiple-choice question with 2 or more options.
 * Demonstrates polymorphism by overriding checkAnswer().
 */
public class MultipleChoiceQuestion extends Question {
    private List<Option> options = new ArrayList<>();

    public MultipleChoiceQuestion() {
        setType(TYPE_MULTIPLE_CHOICE);
    }

    public MultipleChoiceQuestion(int id, int categoryId, String categoryName, String text, Difficulty difficulty, String correctAnswer, List<Option> options) {
        super(id, categoryId, categoryName, TYPE_MULTIPLE_CHOICE, text, difficulty, correctAnswer);
        if (options != null) {
            this.options = new ArrayList<>(options);
        }
    }

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options != null ? options : new ArrayList<>();
    }

    public void addOption(Option option) {
        if (option != null) {
            options.add(option);
            if (option.isCorrect()) {
                setCorrectAnswer(option.getOptionText());
            }
        }
    }

    public void shuffleOptions() {
        if (options != null && options.size() > 1) {
            Collections.shuffle(options);
        }
    }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null || getCorrectAnswer() == null) {
            return false;
        }
        return getCorrectAnswer().trim().equalsIgnoreCase(answer.trim());
    }

    @Override
    public String getCorrectAnswerDisplay() {
        return getCorrectAnswer() != null ? getCorrectAnswer() : "N/A";
    }
}
