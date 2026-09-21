package com.chronoquiz.util;

import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.MultipleChoiceQuestion;
import com.chronoquiz.model.Option;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.ShortAnswerQuestion;
import com.google.gson.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles exporting question banks and attempt results to JSON files using Gson.
 */
public class JsonExporter {
    private final Gson gson;

    public JsonExporter() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Question.class, new QuestionJsonSerializer())
                .create();
    }

    public void exportQuestionsToFile(List<Question> questions, File targetFile) throws IOException {
        try (FileWriter writer = new FileWriter(targetFile)) {
            gson.toJson(questions, writer);
        }
    }

    public void exportAttemptToFile(Attempt attempt, File targetFile) throws IOException {
        try (FileWriter writer = new FileWriter(targetFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(attempt, writer);
        }
    }

    /**
     * Custom serializer to handle polymorphic Question subclasses cleanly.
     */
    private static class QuestionJsonSerializer implements JsonSerializer<Question> {
        @Override
        public JsonElement serialize(Question src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("type", src.getType());
            obj.addProperty("category", src.getCategoryName());
            obj.addProperty("difficulty", src.getDifficulty().name());
            obj.addProperty("text", src.getText());
            obj.addProperty("correctAnswer", src.getCorrectAnswer());

            if (src instanceof MultipleChoiceQuestion mcq) {
                JsonArray optArray = new JsonArray();
                for (Option opt : mcq.getOptions()) {
                    optArray.add(opt.getOptionText());
                }
                obj.add("options", optArray);
            } else if (src instanceof ShortAnswerQuestion saq) {
                JsonArray accArray = new JsonArray();
                for (String acc : saq.getAcceptedAnswers()) {
                    accArray.add(acc);
                }
                obj.add("acceptedAnswers", accArray);
            }

            return obj;
        }
    }
}
