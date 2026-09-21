package com.chronoquiz.api;

import com.chronoquiz.db.CategoryDAO;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.Difficulty;
import com.chronoquiz.model.MultipleChoiceQuestion;
import com.chronoquiz.model.Option;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps OpenTDB API questions to domain MultipleChoiceQuestion objects.
 * Handles URL decoding, option shuffling, and SQLite caching.
 */
public class ApiQuestionMapper {
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();

    public MultipleChoiceQuestion toMultipleChoiceQuestion(ApiQuestion apiQ) {
        if (apiQ == null) return null;

        String decodedCategory = decode(apiQ.getCategory());
        String decodedQuestion = decode(apiQ.getQuestion());
        String decodedCorrect = decode(apiQ.getCorrectAnswer());
        String decodedDiff = decode(apiQ.getDifficulty());

        int categoryId = categoryDAO.getOrCreateCategory(decodedCategory);
        Difficulty difficulty = Difficulty.fromString(decodedDiff);

        List<Option> options = new ArrayList<>();
        options.add(new Option(decodedCorrect, true));

        if (apiQ.getIncorrectAnswers() != null) {
            for (String inc : apiQ.getIncorrectAnswers()) {
                options.add(new Option(decode(inc), false));
            }
        }

        // Shuffle options so correct answer is not always in the first position
        Collections.shuffle(options);

        MultipleChoiceQuestion mcq = new MultipleChoiceQuestion(
                0,
                categoryId,
                decodedCategory,
                decodedQuestion,
                difficulty,
                decodedCorrect,
                options
        );

        // Cache in SQLite if question does not already exist
        cacheLocally(mcq);

        return mcq;
    }

    private void cacheLocally(MultipleChoiceQuestion mcq) {
        try {
            if (!questionDAO.questionExists(mcq.getText())) {
                questionDAO.insertQuestion(mcq);
            }
        } catch (Exception e) {
            System.err.println("Could not cache API question locally: " + e.getMessage());
        }
    }

    private String decode(String text) {
        if (text == null) return "";
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return text;
        }
    }
}
