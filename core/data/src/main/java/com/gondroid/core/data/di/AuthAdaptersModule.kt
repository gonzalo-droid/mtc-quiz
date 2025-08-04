package com.gondroid.core.data.di

import com.gondroid.core.data.adapter.AuthProviderAdapter
import com.gondroid.core.data.adapter.FacebookAuthAdapter
import com.gondroid.core.data.adapter.GoogleAuthAdapter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthAdaptersModule {

    @Binds
    @IntoSet
    abstract fun bindGoogleAuthAdapter(
        googleAuthAdapter: GoogleAuthAdapter
    ): AuthProviderAdapter

    @Binds
    @IntoSet
    abstract fun bindFacebookAuthAdapter(
        facebookAuthAdapter: FacebookAuthAdapter
    ): AuthProviderAdapter
}