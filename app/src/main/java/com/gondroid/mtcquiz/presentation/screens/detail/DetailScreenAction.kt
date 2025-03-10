package com.gondroid.mtcquiz.presentation.screens.detail

sealed interface DetailScreenAction {
    data object GoToEvaluation : DetailScreenAction

    data object GoToQuestions : DetailScreenAction

    data object ShowPDF : DetailScreenAction

    data object GoToConfiguration : DetailScreenAction

    data object Back : DetailScreenAction
}