package com.gondroid.auth.presentation

import com.gondroid.core.domain.provider.AuthProvider

data class LoginState(
    val availableProviders: List<AuthProvider> = emptyList(),
    val isLoading: Boolean = false,
    val isLoggingIn: Boolean = false,
    val error: String
)
