package com.gondroid.mtcquiz.presentation.screens.configuration.customize

sealed interface CustomizeScreenAction {
    data object UpdateValues : CustomizeScreenAction
    data object ResetValues : CustomizeScreenAction
}