package com.gondroid.auth.presentation

sealed interface LoginEvent {
    data object Success : LoginEvent
    data object Fail : LoginEvent
}