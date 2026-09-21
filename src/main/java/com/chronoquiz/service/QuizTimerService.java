package com.chronoquiz.service;

import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages the quiz countdown timer using a background ScheduledExecutorService.
 * Ensures thread-safe communication with the JavaFX Application Thread via Platform.runLater().
 */
public class QuizTimerService {
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTask;

    private final int totalDuration;
    private volatile int timeRemaining;
    private volatile boolean running = false;
    private volatile boolean paused = false;

    private Consumer<Integer> onTick;
    private Runnable onTimeout;

    public QuizTimerService(int totalDurationSeconds) {
        this.totalDuration = Math.max(5, totalDurationSeconds);
        this.timeRemaining = this.totalDuration;
    }

    public void setOnTick(Consumer<Integer> onTick) {
        this.onTick = onTick;
    }

    public void setOnTimeout(Runnable onTimeout) {
        this.onTimeout = onTimeout;
    }

    /**
     * Starts the countdown timer thread.
     */
    public synchronized void start() {
        if (running) return;

        running = true;
        paused = false;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ChronoQuiz-Timer-Thread");
            t.setDaemon(true); // Ensures JVM can exit smoothly if app is closed
            return t;
        });

        scheduledTask = scheduler.scheduleAtFixedRate(() -> {
            if (!running || paused) {
                return;
            }

            timeRemaining--;

            // Update UI on JavaFX Application Thread
            final int currentRemaining = timeRemaining;
            if (onTick != null) {
                Platform.runLater(() -> onTick.accept(currentRemaining));
            }

            // Check boundary condition
            if (currentRemaining <= 0) {
                stop();
                if (onTimeout != null) {
                    Platform.runLater(onTimeout);
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Pauses the timer without resetting time.
     */
    public synchronized void pause() {
        this.paused = true;
    }

    /**
     * Resumes the paused timer.
     */
    public synchronized void resume() {
        this.paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isRunning() {
        return running;
    }

    /**
     * Stops the timer and shuts down the executor thread safely.
     */
    public synchronized void stop() {
        running = false;
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            scheduledTask = null;
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public int getTotalDuration() {
        return totalDuration;
    }

    public double getProgress() {
        if (totalDuration == 0) return 0.0;
        return (double) timeRemaining / (double) totalDuration;
    }

    public String getFormattedTime() {
        int minutes = Math.max(0, timeRemaining) / 60;
        int seconds = Math.max(0, timeRemaining) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
