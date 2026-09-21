package com.chronoquiz.util;

import com.chronoquiz.db.CategoryDAO;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles importing question banks from JSON files into SQLite database.
 */
public class JsonImporter {
    private final CategoryDAO categoryDAO = new CategoryDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();

    /**
     * Imports questions from a JSON file into the local SQLite database.
     * @return count of newly inserted questions.
     */
    public int importQuestionsFromFile(File jsonFile) throws IOException {
        int importedCount = 0;

        try (FileReader reader = new FileReader(jsonFile)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                throw new IllegalArgumentException("JSON root must be an array of questions.");
            }

            JsonArray array = root.getAsJsonArray();
            for (JsonElement elem : array) {
                if (!elem.isJsonObject()) continue;
                JsonObject obj = elem.getAsJsonObject();

                String text = obj.has("text") ? obj.get("text").getAsString() : "";
                if (text.trim().isEmpty() || questionDAO.questionExists(text)) {
                    continue; // Skip invalid or duplicate questions
                }

                String type = obj.has("type") ? obj.get("type").getAsString() : Question.TYPE_MULTIPLE_CHOICE;
                String category = obj.has("category") ? obj.get("category").getAsString() : "General Knowledge";
                String difficultyStr = obj.has("difficulty") ? obj.get("difficulty").getAsString() : "MEDIUM";
                String correctAnswer = obj.has("correctAnswer") ? obj.get("correctAnswer").getAsString() : "";

                int catId = categoryDAO.getOrCreateCategory(category);
                Difficulty difficulty = Difficulty.fromString(difficultyStr);

                if (Question.TYPE_SHORT_ANSWER.equalsIgnoreCase(type)) {
                    List<String> accepted = new ArrayList<>();
                    if (obj.has("acceptedAnswers") && obj.get("acceptedAnswers").isJsonArray()) {
                        for (JsonElement item : obj.getAsJsonArray("acceptedAnswers")) {
                            accepted.add(item.getAsString());
                        }
                    }
                    if (correctAnswer != null && !accepted.contains(correctAnswer)) {
                        accepted.add(correctAnswer);
                    }

                    ShortAnswerQuestion saq = new ShortAnswerQuestion(0, catId, category, text, difficulty, correctAnswer, accepted);
                    if (questionDAO.insertQuestion(saq)) {
                        importedCount++;
                    }
                } else {
                    List<Option> options = new ArrayList<>();
                    if (obj.has("options") && obj.get("options").isJsonArray()) {
                        for (JsonElement item : obj.getAsJsonArray("options")) {
                            String optText = item.getAsString();
                            boolean isCorrect = optText.trim().equalsIgnoreCase(correctAnswer.trim());
                            options.add(new Option(optText, isCorrect));
                        }
                    }

                    // If correct answer wasn't in options, add it
                    boolean foundCorrect = options.stream().anyMatch(Option::isCorrect);
                    if (!foundCorrect && !correctAnswer.isEmpty()) {
                        options.add(new Option(correctAnswer, true));
                    }

                    MultipleChoiceQuestion mcq = new MultipleChoiceQuestion(0, catId, category, text, difficulty, correctAnswer, options);
                    if (questionDAO.insertQuestion(mcq)) {
                        importedCount++;
                    }
                }
            }
        }

        return importedCount;
    }
}
