package com.gondroid.mtcquiz.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.domain.repository.AuthRepository
import com.gondroid.mtcquiz.domain.repository.PreferenceRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val preferenceRepository: PreferenceRepository,
) : AuthRepository {

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun logout() {
        firebaseAuth.signOut()
        // Create a ClearCredentialStateRequest object
        val clearRequest = ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)
        // On user log-out, clear the restore key
        credentialManager.clearCredentialState(clearRequest)
    }

    override suspend fun signInWithGoogle(idToken: String) : Boolean {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            result?.user?.let { user ->
                saveUserData(user)
            }
            true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error en signInWithGoogle", e)
            false
        }
    }

    private suspend fun saveUserData(user: FirebaseUser?) {
        preferenceRepository.setUserName(user?.displayName ?: "")
        preferenceRepository.setUserName(user?.displayName ?: "")
        preferenceRepository.setPhotoUrl(user?.photoUrl.toString())
        preferenceRepository.setIsLoggedIn(true)
    }

    override suspend fun getGoogleClient(context: Context): GetCredentialResponse {
        val signInWithGoogleOption: GetSignInWithGoogleOption =
            GetSignInWithGoogleOption.Builder(
                context.getString(R.string.default_web_client_id)
            ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return credentialManager.getCredential(context, request)
    }

}
