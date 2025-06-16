package com.gondroid.detail.presentation

sealed interface DetailAction {
    data class GoToEvaluation(val categoryId: String) : DetailAction

    data class GoToQuestions(val categoryId: String) : DetailAction

    data class ShowPDF(val categoryId: String) : DetailAction

    data object GoToConfiguration : DetailAction

    data object Back : DetailAction
}