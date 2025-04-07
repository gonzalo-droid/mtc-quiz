package com.gondroid.mtcquiz.domain.repository

import androidx.credentials.GetCredentialResponse

interface AuthRepository {

    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit)

    fun logout()

    fun isUserLoggedIn(): Boolean

    suspend fun getGoogleClient(): GetCredentialResponse

}