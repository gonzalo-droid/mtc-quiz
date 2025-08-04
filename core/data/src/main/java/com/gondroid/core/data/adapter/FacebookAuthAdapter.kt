package com.gondroid.core.data.adapter

import android.content.Context
import androidx.credentials.CredentialManager
import com.gondroid.core.domain.provider.AuthProvider
import com.gondroid.core.domain.provider.AuthResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FacebookAuthAdapter @Inject constructor(
    private val credentialManager: CredentialManager,
    @ApplicationContext private val context: Context
) : AuthProviderAdapter {
    override suspend fun authenticate(provider: AuthProvider): AuthResult {
        TODO("Not yet implemented")
    }

    override fun supports(provider: AuthProvider): Boolean {
        TODO("Not yet implemented")
    }

}