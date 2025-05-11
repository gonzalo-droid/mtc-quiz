package com.gondroid.mtcquiz.presentation.screens.configuration


sealed interface ConfigurationEvent {
    data object Success : ConfigurationEvent
}