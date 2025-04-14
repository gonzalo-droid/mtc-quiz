package com.gondroid.mtcquiz.presentation.screens.login

sealed interface LoginEvent {
    data object Success : LoginEvent
    data object Fail : LoginEvent
}