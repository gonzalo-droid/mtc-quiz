package com.gondroid.core.data.repository

import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.gondroid.core.data.adapter.AuthProviderAdapter
import com.gondroid.core.domain.provider.AuthProvider
import com.gondroid.core.domain.provider.AuthResult
import com.gondroid.core.domain.repository.AuthRepository
import com.gondroid.core.domain.repository.PreferenceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val preferenceRepository: PreferenceRepository,
    private val adapters: Set<@JvmSuppressWildcards AuthProviderAdapter>
) : AuthRepository {

    override suspend fun isAuthenticated(): Boolean = firebaseAuth.currentUser != null

    override suspend fun logout(): Boolean {
        return try {
            firebaseAuth.signOut()
            // Create a ClearCredentialStateRequest object
            val clearRequest =
                ClearCredentialStateRequest(ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL)
            // On user log-out, clear the restore key
            credentialManager.clearCredentialState(clearRequest)

            preferenceRepository.logout()
            true
        } catch (e: Exception) {
            Timber.Forest.tag("AuthRepository").e(e, "Error en logout")
            false
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Boolean {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            result?.user?.let { user ->
                saveUserData(user)
            }
            true
        } catch (e: Exception) {
            Timber.Forest.tag("AuthRepository").e(e, "Error en signInWithGoogle")
            false
        }
    }

    private suspend fun saveUserData(user: FirebaseUser?) {
        preferenceRepository.setUserName(user?.displayName ?: "")
        preferenceRepository.setUserName(user?.displayName ?: "")
        preferenceRepository.setPhotoUrl(user?.photoUrl.toString())
        preferenceRepository.setIsLoggedIn(true)
    }

    override suspend fun authenticate(provider: AuthProvider): AuthResult {
        val adapter = adapters.find { it.supports(provider) }
            ?: return AuthResult.Error("No adapter found for provider: ${provider::class.simpleName}")

        return adapter.authenticate(provider)
    }


    override suspend fun getAvailableProviders(): List<AuthProvider> {
        TODO("Not yet implemented")
    }

}