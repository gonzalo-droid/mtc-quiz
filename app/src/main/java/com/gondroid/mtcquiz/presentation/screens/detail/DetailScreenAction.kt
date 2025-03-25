package com.gondroid.mtcquiz.presentation.screens.detail

sealed interface DetailScreenAction {
    data class GoToEvaluation(val categoryId: String) : DetailScreenAction

    data class GoToQuestions(val categoryId: String) : DetailScreenAction

    data object ShowPDF : DetailScreenAction

    data object GoToConfiguration : DetailScreenAction

    data object Back : DetailScreenAction
}