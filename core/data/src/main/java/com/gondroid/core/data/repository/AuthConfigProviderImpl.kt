package com.gondroid.core.data.repository

import android.content.Context
import com.gondroid.core.data.R
import com.gondroid.core.domain.provider.AuthConfigProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthConfigProviderImpl @Inject constructor(@ApplicationContext private val context: Context) :
    AuthConfigProvider {

    override suspend fun getGoogleClientId(): String =
        context.getString(R.string.default_web_client_id)

    override suspend fun getFacebookAppId(): String =
        context.getString(R.string.default_web_client_id)

    override suspend fun getAppleServiceId(): String =
        context.getString(R.string.default_web_client_id)
}
