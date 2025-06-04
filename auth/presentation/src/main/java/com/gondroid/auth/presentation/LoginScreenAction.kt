package com.gondroid.auth.presentation

sealed interface LoginScreenAction {
    data object GoogleSignOn : LoginScreenAction
}