package com.gondroid.presentation.screens.configuration.customize


sealed interface CustomizeEvent {
    data object Success : CustomizeEvent
    data object Error : CustomizeEvent
}