package com.chronoquiz.model;

/**
 * Represents quiz difficulty levels.
 */
public enum Difficulty {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    ANY("Any Difficulty");

    private final String displayName;

    Difficulty(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Difficulty fromString(String text) {
        if (text == null) return ANY;
        for (Difficulty d : Difficulty.values()) {
            if (d.name().equalsIgnoreCase(text) || d.displayName.equalsIgnoreCase(text)) {
                return d;
            }
        }
        return ANY;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
