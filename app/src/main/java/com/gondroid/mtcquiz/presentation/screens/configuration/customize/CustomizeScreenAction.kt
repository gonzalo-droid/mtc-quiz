package com.gondroid.mtcquiz.presentation.screens.configuration.customize

sealed interface CustomizeScreenAction {
    data object GoToTerm : CustomizeScreenAction

}