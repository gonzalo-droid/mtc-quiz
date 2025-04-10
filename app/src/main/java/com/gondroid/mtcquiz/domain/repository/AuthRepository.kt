package com.gondroid.mtcquiz.domain.repository

import android.app.Activity
import android.content.Context
import androidx.credentials.GetCredentialResponse

interface AuthRepository {

    fun signInWithGoogle(idToken: String, onResult: (Boolean, String?) -> Unit)

    fun logout()

    fun isUserLoggedIn(): Boolean

    suspend fun getGoogleClient(activity: Activity?): GetCredentialResponse

    suspend fun signWithGoogle(context: Context): GetCredentialResponse
}