package com.gondroid.detail.presentation

sealed interface DetailScreenAction {
    data class GoToEvaluation(val categoryId: String) : DetailScreenAction

    data class GoToQuestions(val categoryId: String) : DetailScreenAction

    data class ShowPDF(val categoryId: String) : DetailScreenAction

    data object GoToConfiguration : DetailScreenAction

    data object Back : DetailScreenAction
}