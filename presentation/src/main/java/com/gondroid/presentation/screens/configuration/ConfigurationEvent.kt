package com.gondroid.presentation.screens.configuration


sealed interface ConfigurationEvent {
    data object Success : ConfigurationEvent
}