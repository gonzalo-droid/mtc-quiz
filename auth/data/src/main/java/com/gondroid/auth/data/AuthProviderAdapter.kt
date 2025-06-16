package com.gondroid.auth.data

import com.gondroid.auth.domain.provider.AuthProvider
import com.gondroid.auth.domain.provider.AuthResult

interface AuthProviderAdapter {
    suspend fun authenticate(provider: AuthProvider): AuthResult
    fun supports(provider: AuthProvider): Boolean
}