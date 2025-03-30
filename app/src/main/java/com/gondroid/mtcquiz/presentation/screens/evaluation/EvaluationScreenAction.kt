package com.gondroid.mtcquiz.presentation.screens.evaluation

sealed interface EvaluationScreenAction {
    data object VerifyAnswer : EvaluationScreenAction
    data object Back : EvaluationScreenAction
    data object NextQuestion : EvaluationScreenAction
    data object FinishExam : EvaluationScreenAction
    data class SummaryExam(val categoryId: String) : EvaluationScreenAction
    data class SaveAnswer(val isCorrect: Boolean, val option: String) : EvaluationScreenAction
}