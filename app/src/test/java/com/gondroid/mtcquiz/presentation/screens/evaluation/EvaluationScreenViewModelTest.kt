package com.gondroid.mtcquiz.presentation.screens.evaluation

import org.junit.Test

class EvaluationScreenViewModelTest {

    @Test
    fun `getState initial state`() {
        // Verify that the initial state of `state` is `EvaluationDataState()`.
        // TODO implement test
    }

    @Test
    fun `getState category loaded`() {
        // Verify that after init block category data is loaded in state.
        // TODO implement test
    }

    @Test
    fun `getState questions loaded`() {
        // Verify that after init block questions data is loaded in state.
        // TODO implement test
    }

    @Test
    fun `getState category and question load fail`() {
        // Check if state is not modified when repository.getCategoryById or 
        // repository.getQuestionsByCategory return null or empty.
        // TODO implement test
    }

    @Test
    fun `getResultsList initial state`() {
        // Verify that the initial state of `resultsList` is an empty list.
        // TODO implement test
    }

    @Test
    fun `getResultsList after saveAnswer`() {
        // Verify that `resultsList` contains the correct `QuestionResult` 
        // after calling `saveAnswer` with `answerWasVerified` as true.
        // TODO implement test
    }

    @Test
    fun `getResultsList after multiple saveAnswer`() {
        // Verify that `resultsList` contains all the `QuestionResult` 
        // after calling `saveAnswer` multiple times.
        // TODO implement test
    }

    @Test
    fun `getEvent initial state`() {
        // Verify that the initial state of `event` does not emit any events.
        // TODO implement test
    }

    @Test
    fun `getEvent after saveExam success`() {
        // Verify that `event` emits `EvaluationEvent.EvaluationCreated` 
        // with correct `evaluationId` after calling `saveExam`.
        // TODO implement test
    }

    @Test
    fun `nextQuestion valid index`() {
        // Verify that `indexQuestion` increments by 1, 
        // `answerWasSelected` and `answerWasVerified` reset to false and 
        // the correct next question is loaded.
        // TODO implement test
    }

    @Test
    fun `nextQuestion last question`() {
        // Verify that when we are on last question and we run `nextQuestion()` the question changed 
        // correctly to the next one.
        // TODO implement test
    }

    @Test
    fun `verifyAnswer`() {
        // Verify that `answerWasSelected` and `answerWasVerified` are set to true.
        // TODO implement test
    }

    @Test
    fun `verifyAnswer last question`() {
        // Verify that when called on the last question, `isFinishExam` is set to true.
        // TODO implement test
    }

    @Test
    fun `saveAnswer valid`() {
        // Verify that if `answerWasVerified` is true, 
        // a `QuestionResult` is added to `resultsList` with correct data.
        // TODO implement test
    }

    @Test
    fun `saveAnswer invalid`() {
        // Verify that if `answerWasVerified` is false, no `QuestionResult` is added to `resultsList`.
        // TODO implement test
    }

    @Test
    fun `saveExam success`() {
        // Verify that `repository.saveEvaluation` is called with correct data, 
        // and `event` emits `EvaluationEvent.EvaluationCreated`.
        // TODO implement test
    }

    @Test
    fun `saveExam edge case approved`() {
        // Verify that with 80% or more correct answer exam state is approved.
        // TODO implement test
    }

    @Test
    fun `saveExam edge case rejected`() {
        // Verify that with less than 80% correct answer exam state is rejected.
        // TODO implement test
    }

    @Test
    fun `saveExam no questions`() {
        // Verify the behavior when there are no questions 
        // and `saveExam` is called, it should save with zero values.
        // TODO implement test
    }

    @Test
    fun `saveExam only correct questions`() {
        // Verify the behavior when there are all correct answer and `saveExam` is called.
        // TODO implement test
    }

    @Test
    fun `saveExam only incorrect questions`() {
        // Verify the behavior when there are all incorrect answer and `saveExam` is called.
        // TODO implement test
    }

    @Test
    fun `saveExam repository exception`() {
        // Verify the behavior when `repository.saveEvaluation` throws an exception.
        // TODO implement test
    }

}