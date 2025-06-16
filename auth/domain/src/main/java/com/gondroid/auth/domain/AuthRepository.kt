package com.gondroid.auth.domain

import com.gondroid.auth.domain.provider.AuthCredential
import com.gondroid.auth.domain.provider.AuthProvider
import com.gondroid.auth.domain.provider.AuthResult


interface AuthRepository {

    suspend fun signInWithGoogle(idToken: String): Boolean

    suspend fun logout(): Boolean

    suspend fun isAuthenticated(): Boolean

    suspend fun authenticate(provider: AuthProvider): AuthResult

    // suspend fun logout(): Result<Unit>
    suspend fun getAvailableProviders(): List<AuthProvider>
}