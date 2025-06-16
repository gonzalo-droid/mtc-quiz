package com.gondroid.auth.data

import android.content.Context
import com.gondroid.auth.domain.provider.AuthConfigProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthConfigProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AuthConfigProvider {

    override suspend fun getGoogleClientId(): String {
        return context.getString(R.string.default_web_client_id)
    }

    override suspend fun getFacebookAppId(): String {
        return context.getString(R.string.default_web_client_id)
    }

    override suspend fun getAppleServiceId(): String {
        return context.getString(R.string.default_web_client_id)
    }
}