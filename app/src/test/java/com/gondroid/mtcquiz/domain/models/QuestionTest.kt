package com.gondroid.mtcquiz.domain.models

import org.junit.Before
import org.junit.Test

class QuestionTest {

    private lateinit var question: Question

    @Before
    fun setup() {
        question = Question(
            id = 1,
            topic = "Sample Topic",
            title = "What is 2 + 2?",
            answer = "b",  // index 1
            options = listOf("3", "4", "5", "6") // index 1 = correct, index start -> 0 (a)
        )
    }

    @Test
    fun givenQuestion_whenGetOption_thenReturnsCorrectOption() {
        val result = question.isCorrectAnswer(1)
        assert(result)
    }

    @Test
    fun givenQuestion_whenGetOption_thenReturnsIncorrectOption() {
        val result = question.isCorrectAnswer(0)
        assert(!result)
    }

    @Test
    fun givenQuestion_whenGetOption_thenReturnsCorrectOptionText() {
        val result = question.getOption("b")
        assert(result == "4")
    }

    @Test
    fun givenQuestion_whenGetOption_thenReturnsIncorrectOptionText() {
        val result = question.getOption("a")
        assert(result == "3")
    }

}