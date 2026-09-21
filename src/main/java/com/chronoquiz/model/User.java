package com.chronoquiz.model;

import java.time.LocalDateTime;

/**
 * Represents a player / user who takes quizzes.
 */
public class User {
    private int id;
    private String name;
    private String createdAt;

    public User() {
    }

    public User(int id, String name, String createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public User(String name) {
        this(0, name, LocalDateTime.now().toString());
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return name;
    }
}
