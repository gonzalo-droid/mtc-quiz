package com.gondroid.auth.presentation

sealed interface LoginAction {
    data object GoogleSignOn : LoginAction
}