package com.gondroid.core.domain.repository

import com.gondroid.core.domain.provider.AuthProvider
import com.gondroid.core.domain.provider.AuthResult

interface AuthRepository {

    suspend fun signInWithGoogle(idToken: String): Boolean

    suspend fun logout(): Boolean

    suspend fun isAuthenticated(): Boolean

    suspend fun authenticate(provider: AuthProvider): AuthResult

    // suspend fun logout(): Result<Unit>
    suspend fun getAvailableProviders(): List<AuthProvider>
}