package com.chronoquiz.model;

import java.util.Objects;

/**
 * Represents a quiz category.
 */
public class Category {
    private int id;
    private String name;

    public Category() {
    }

    public Category(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Category(String name) {
        this(0, name);
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
