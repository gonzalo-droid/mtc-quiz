package com.gondroid.auth.domain


interface AuthRepository {

    suspend fun signInWithGoogle(idToken: String): Boolean

    suspend fun logout(): Boolean

    fun isUserLoggedIn(): Boolean
}