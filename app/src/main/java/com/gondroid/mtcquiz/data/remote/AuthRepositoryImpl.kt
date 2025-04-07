package com.gondroid.mtcquiz.data.remote

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.gondroid.mtcquiz.R
import com.gondroid.mtcquiz.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val context: Context
) : AuthRepository {

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null
    override fun logout(): Unit = firebaseAuth.signOut()

    override suspend fun getGoogleClient(activity : Activity?): GetCredentialResponse {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .setFilterByAuthorizedAccounts(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return credentialManager.getCredential(activity!!, request)
    }

    override fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

}
