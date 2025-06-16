package com.gondroid.auth.data.di

import com.gondroid.auth.data.AuthProviderAdapter
import com.gondroid.auth.data.FacebookAuthAdapter
import com.gondroid.auth.data.GoogleAuthAdapter
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