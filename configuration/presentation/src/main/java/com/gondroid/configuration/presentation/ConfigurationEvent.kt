package com.gondroid.configuration.presentation


sealed interface ConfigurationEvent {
    data object Success : ConfigurationEvent
}