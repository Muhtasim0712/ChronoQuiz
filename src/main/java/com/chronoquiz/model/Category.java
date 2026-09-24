package com.chronoquiz.model;

import java.util.Objects;

/**
 * Represents a quiz category with name, curriculum description, and usage metrics.
 */
public class Category {
    private int id;
    private String name;
    private String description;
    private int questionCount;
    private int attemptCount;

    public Category() {
        this.description = "";
    }

    public Category(int id, String name) {
        this(id, name, "");
    }

    public Category(String name) {
        this(0, name, "");
    }

    public Category(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
    }

    public Category(String name, String description) {
        this(0, name, description);
    }

    public Category(int id, String name, String description, int questionCount, int attemptCount) {
        this.id = id;
        this.name = name;
        this.description = description != null ? description : "";
        this.questionCount = questionCount;
        this.attemptCount = attemptCount;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return id == category.id || (name != null && name.equalsIgnoreCase(category.name));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name != null ? name.toLowerCase() : "");
    }

    @Override
    public String toString() {
        return name;
    }
}
