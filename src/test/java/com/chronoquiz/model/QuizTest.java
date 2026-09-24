package com.chronoquiz.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class QuizTest {

    @Test
    public void testQuizInitializationAndNavigation() {
        MultipleChoiceQuestion q1 = new MultipleChoiceQuestion(
                1, 1, "CS", "Question 1?", Difficulty.EASY, "A",
                Arrays.asList(new Option("A", true), new Option("B", false))
        );
        ShortAnswerQuestion q2 = new ShortAnswerQuestion(
                2, 1, "CS", "Question 2?", Difficulty.EASY, "Answer",
                Arrays.asList("Answer", "Ans")
        );

        Quiz quiz = new Quiz("Student1", "CS", 1, Difficulty.EASY, Arrays.asList(q1, q2), 60);

        assertEquals("Student1", quiz.getUserName());
        assertEquals(2, quiz.getTotalQuestions());
        assertEquals(0, quiz.getCurrentIndex());
        assertEquals(q1, quiz.getCurrentQuestion());
        assertTrue(quiz.hasNext());

        quiz.nextQuestion();
        assertEquals(1, quiz.getCurrentIndex());
        assertEquals(q2, quiz.getCurrentQuestion());
        assertFalse(quiz.hasNext());
    }

    @Test
    public void testScoringCalculation() {
        MultipleChoiceQuestion q1 = new MultipleChoiceQuestion(
                1, 1, "CS", "Question 1?", Difficulty.EASY, "Stack",
                Arrays.asList(new Option("Stack", true), new Option("Queue", false))
        );
        q1.setUserAnswer("Stack"); // Correct

        ShortAnswerQuestion q2 = new ShortAnswerQuestion(
                2, 1, "CS", "Question 2?", Difficulty.EASY, "extends",
                Arrays.asList("extends")
        );
        q2.setUserAnswer("implements"); // Incorrect

        Quiz quiz = new Quiz("Student1", "CS", 1, Difficulty.EASY, Arrays.asList(q1, q2), 60);
        quiz.submit();

        assertEquals(1, quiz.calculateScore());
        assertEquals(60, quiz.getTotalTimeSeconds());
    }
}
