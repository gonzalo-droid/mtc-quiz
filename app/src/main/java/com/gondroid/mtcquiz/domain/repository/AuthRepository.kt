package com.gondroid.mtcquiz.domain.repository

import android.content.Context
import androidx.credentials.GetCredentialResponse

interface AuthRepository {

    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit)

    suspend fun logout()

    fun isUserLoggedIn(): Boolean

    suspend fun getGoogleClient(context: Context): GetCredentialResponse
}