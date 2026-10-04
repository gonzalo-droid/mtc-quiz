package com.gondroid.evaluation.presentation

sealed interface EvaluationAction {
    data class VerifyAnswer(val isCorrect: Boolean, val option: String) : EvaluationAction
    data object Back : EvaluationAction
    data object NextQuestion : EvaluationAction
    data class SummaryExam(val categoryId: String) : EvaluationAction
    data object ConfirmCancel : EvaluationAction
    data object DismissDialog : EvaluationAction
}
