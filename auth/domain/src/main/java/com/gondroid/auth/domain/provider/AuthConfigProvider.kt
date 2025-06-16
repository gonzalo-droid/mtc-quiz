package com.gondroid.auth.domain.provider

interface AuthConfigProvider {
    suspend fun getGoogleClientId(): String
    suspend fun getFacebookAppId(): String
    suspend fun getAppleServiceId(): String
}