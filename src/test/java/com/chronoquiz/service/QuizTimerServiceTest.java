package com.chronoquiz.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QuizTimerServiceTest {

    @Test
    public void testTimerInitialValuesAndFormatting() {
        QuizTimerService timer = new QuizTimerService(90);
        assertEquals("01:30", timer.getFormattedTime());
        assertEquals(90, timer.getTimeRemaining());
        assertEquals(90, timer.getTotalDuration());
        assertEquals(1.0, timer.getProgress(), 0.001);
        assertFalse(timer.isRunning());
        assertFalse(timer.isPaused());
    }

    @Test
    public void testTimerMinimumDuration() {
        QuizTimerService timer = new QuizTimerService(2);
        // Minimum allowed duration is 5 seconds
        assertEquals(5, timer.getTotalDuration());
        assertEquals("00:05", timer.getFormattedTime());
    }
}
