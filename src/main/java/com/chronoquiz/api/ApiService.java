package com.chronoquiz.api;

import com.chronoquiz.model.Difficulty;
import com.chronoquiz.model.Question;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service to asynchronously fetch questions from Open Trivia DB using java.net.http.HttpClient.
 */
public class ApiService {
    private static final String BASE_URL = "https://opentdb.com/api.php";
    private final HttpClient httpClient;
    private final Gson gson;
    private final ApiQuestionMapper mapper;

    public ApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        this.gson = new Gson();
        this.mapper = new ApiQuestionMapper();
    }

    /**
     * Builds request URL and asynchronously fetches questions.
     */
    public CompletableFuture<List<Question>> fetchQuestionsAsync(int amount, Integer categoryId, Difficulty difficulty) {
        StringBuilder url = new StringBuilder(BASE_URL);
        url.append("?amount=").append(Math.max(1, amount));

        int apiCat = mapCategoryToOpenTdb(categoryId);
        if (apiCat > 0) {
            url.append("&category=").append(apiCat);
        }

        if (difficulty != null && difficulty != Difficulty.ANY) {
            url.append("&difficulty=").append(difficulty.name().toLowerCase());
        }

        url.append("&type=multiple&encode=url3986");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("API returned HTTP " + response.statusCode());
                    }

                    try {
                        ApiResponse apiResponse = gson.fromJson(response.body(), ApiResponse.class);
                        if (apiResponse == null) {
                            throw new RuntimeException("Empty response body from trivia API.");
                        }

                        if (apiResponse.getResponseCode() != 0) {
                            String errorMsg = switch (apiResponse.getResponseCode()) {
                                case 1 -> "No questions found matching criteria.";
                                case 2 -> "Invalid query parameter in request.";
                                case 5 -> "Rate limit reached. Please wait a few seconds.";
                                default -> "API Error code: " + apiResponse.getResponseCode();
                            };
                            throw new RuntimeException(errorMsg);
                        }

                        List<Question> questions = new ArrayList<>();
                        if (apiResponse.getResults() != null) {
                            for (ApiQuestion apiQ : apiResponse.getResults()) {
                                questions.add(mapper.toMultipleChoiceQuestion(apiQ));
                            }
                        }

                        if (questions.isEmpty()) {
                            throw new RuntimeException("No questions returned in the results list.");
                        }

                        return questions;
                    } catch (JsonSyntaxException e) {
                        throw new RuntimeException("Malformed JSON received from API: " + e.getMessage());
                    }
                });
    }

    /**
     * Maps local category selection to known Open Trivia DB category IDs.
     */
    private int mapCategoryToOpenTdb(Integer categoryId) {
        if (categoryId == null || categoryId <= 0) return 0;
        return switch (categoryId) {
            case 1 -> 18; // Computer Science
            case 2 -> 17; // Science & Nature
            case 3 -> 23; // History
            case 4 -> 9;  // General Knowledge
            default -> 0; // Any category
        };
    }
}
