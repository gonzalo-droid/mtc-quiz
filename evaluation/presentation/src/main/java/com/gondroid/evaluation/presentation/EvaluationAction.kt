package com.gondroid.evaluation.presentation

sealed interface EvaluationAction {
    data object VerifyAnswer : EvaluationAction
    data object Back : EvaluationAction
    data object NextQuestion : EvaluationAction
    data class SummaryExam(val categoryId: String) : EvaluationAction
    data class SaveAnswer(val isCorrect: Boolean, val option: String) : EvaluationAction
    data object ConfirmCancel: EvaluationAction
    data object DismissDialog : EvaluationAction
}