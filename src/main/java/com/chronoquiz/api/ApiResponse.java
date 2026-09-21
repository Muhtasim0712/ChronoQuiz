package com.chronoquiz.api;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Root response object returned by the Open Trivia DB API.
 */
public class ApiResponse {
    @SerializedName("response_code")
    private int responseCode;

    @SerializedName("results")
    private List<ApiQuestion> results;

    public ApiResponse() {
    }

    public ApiResponse(int responseCode, List<ApiQuestion> results) {
        this.responseCode = responseCode;
        this.results = results;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public List<ApiQuestion> getResults() {
        return results;
    }

    public void setResults(List<ApiQuestion> results) {
        this.results = results;
    }
}
