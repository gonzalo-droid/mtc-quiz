package com.gondroid.core.domain.provider

interface AuthConfigProvider {
    suspend fun getGoogleClientId(): String
    suspend fun getFacebookAppId(): String
    suspend fun getAppleServiceId(): String
}