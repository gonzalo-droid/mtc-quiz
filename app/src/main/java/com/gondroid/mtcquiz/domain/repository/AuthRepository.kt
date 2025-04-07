package com.gondroid.mtcquiz.domain.repository

import android.app.Activity
import androidx.credentials.GetCredentialResponse

interface AuthRepository {

    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit)

    fun logout()

    fun isUserLoggedIn(): Boolean

    suspend fun getGoogleClient(activity : Activity?): GetCredentialResponse

}