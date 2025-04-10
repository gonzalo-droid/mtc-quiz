package com.gondroid.mtcquiz.presentation.screens.login

sealed interface LoginScreenAction {
    data object GoogleSignOn : LoginScreenAction
    data object Logout : LoginScreenAction
}