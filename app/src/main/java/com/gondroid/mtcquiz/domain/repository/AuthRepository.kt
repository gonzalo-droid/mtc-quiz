package com.gondroid.mtcquiz.domain.repository

import android.content.Context
import androidx.credentials.GetCredentialResponse

interface AuthRepository {

    suspend fun signInWithGoogle(idToken: String): Boolean

    suspend fun logout()

    fun isUserLoggedIn(): Boolean

    suspend fun getGoogleClient(context: Context): GetCredentialResponse
}