package com.gondroid.configuration.presentation.customize


sealed interface CustomizeEvent {
    data object Success : CustomizeEvent
    data object Error : CustomizeEvent
}