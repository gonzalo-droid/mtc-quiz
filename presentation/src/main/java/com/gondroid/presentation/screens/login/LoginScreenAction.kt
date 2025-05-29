package com.gondroid.presentation.screens.login

sealed interface LoginScreenAction {
    data object GoogleSignOn : LoginScreenAction
}