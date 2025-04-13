package com.gondroid.mtcquiz.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.ClearCredentialStateRequest.Companion.TYPE_CLEAR_RESTORE_CREDENTIAL
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.gondroid.mtcquiz.domain.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val credentialManager: CredentialManager,
    private val context: Context
) : AuthRepository {

    override fun isUserLoggedIn(): Boolean = firebaseAuth.currentUser != null

    override suspend fun logout() {
        firebaseAuth.signOut()

        // Create a ClearCredentialStateRequest object
        val clearRequest = ClearCredentialStateRequest(TYPE_CLEAR_RESTORE_CREDENTIAL)

        // On user log-out, clear the restore key
        credentialManager.clearCredentialState(clearRequest)
    }

    override fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("LoginVM", task.result?.user?.email.toString())
                    Log.d("LoginVM", task.result?.user?.displayName.toString())
                    Log.d("LoginVM", task.result?.user?.photoUrl.toString())
                    Log.d("LoginVM", task.result?.user.toString())
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.message)
                }
            }
    }

    override suspend fun getGoogleClient(context: Context): GetCredentialResponse {
        val signInWithGoogleOption: GetSignInWithGoogleOption =
            GetSignInWithGoogleOption.Builder(
                "949408476336-mk6sb9oqvrpd2fq7fvd2vnu7k70dcadj.apps.googleusercontent.com"
            ).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()

        return credentialManager.getCredential(context, request)
    }

}
