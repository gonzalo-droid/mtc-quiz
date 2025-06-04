package com.gondroid.evaluation.presentation

sealed interface EvaluationScreenAction {
    data object VerifyAnswer : EvaluationScreenAction
    data object Back : EvaluationScreenAction
    data object NextQuestion : EvaluationScreenAction
    data class SummaryExam(val categoryId: String) : EvaluationScreenAction
    data class SaveAnswer(val isCorrect: Boolean, val option: String) : EvaluationScreenAction
    data object ConfirmCancel: EvaluationScreenAction
    data object DismissDialog : EvaluationScreenAction
}