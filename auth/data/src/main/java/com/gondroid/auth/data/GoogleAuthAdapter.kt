package com.gondroid.auth.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.gondroid.auth.domain.provider.AuthCredential
import com.gondroid.auth.domain.provider.AuthProvider
import com.gondroid.auth.domain.provider.AuthResult
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleAuthAdapter @Inject constructor(
    private val credentialManager: CredentialManager,
    @ApplicationContext private val context: Context
) : AuthProviderAdapter {

    override suspend fun authenticate(provider: AuthProvider): AuthResult {
        if (provider !is AuthProvider.Google) {
            return AuthResult.Error("Invalid provider for Google adapter")
        }

        return try {
            val signInOption = GetSignInWithGoogleOption.Builder(provider.clientId).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val response = credentialManager.getCredential(context, request)
            processGoogleResponse(response)

        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: Exception) {
            AuthResult.Error("Google authentication failed: ${e.message}")
        }
    }

    override fun supports(provider: AuthProvider): Boolean = provider is AuthProvider.Google

    private fun processGoogleResponse(response: GetCredentialResponse): AuthResult {
        return when (val credential = response.credential) {
            is GoogleIdTokenCredential -> {
                AuthResult.Success(
                    AuthCredential(
                        token = credential.idToken,
                        provider = "google",
                        email = credential.id,
                        userId = credential.id,
                        displayName = credential.displayName
                    )
                )
            }

            else -> AuthResult.Error("Invalid Google credential type")
        }
    }
}