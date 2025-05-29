package com.gondroid.domain.repository

import android.content.Context
import androidx.credentials.GetCredentialResponse

interface AuthRepository {

    suspend fun signInWithGoogle(idToken: String): Boolean

    suspend fun logout(): Boolean

    fun isUserLoggedIn(): Boolean

    suspend fun getGoogleClient(context: Context): GetCredentialResponse
}