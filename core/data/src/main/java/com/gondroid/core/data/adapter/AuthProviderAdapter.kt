package com.gondroid.core.data.adapter

import com.gondroid.core.domain.provider.AuthProvider
import com.gondroid.core.domain.provider.AuthResult

interface AuthProviderAdapter {

    suspend fun authenticate(provider: AuthProvider): AuthResult

    fun supports(provider: AuthProvider): Boolean
}