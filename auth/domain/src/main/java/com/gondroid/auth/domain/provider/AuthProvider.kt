package com.gondroid.auth.domain.provider

sealed class AuthProvider {
    data class Google(val clientId: String) : AuthProvider()
    data class Facebook(val appId: String) : AuthProvider()
    data class Apple(val serviceId: String) : AuthProvider()
    data class Twitter(val apiKey: String, val apiSecret: String) : AuthProvider()
}

data class AuthCredential(
    val token: String,
    val provider: String,
    val email: String? = null,
    val userId: String? = null,
    val displayName: String? = null
)

sealed class AuthResult {
    data class Success(val credential: AuthCredential) : AuthResult()
    data class Error(val message: String, val code: String? = null) : AuthResult()
    object Cancelled : AuthResult()
}