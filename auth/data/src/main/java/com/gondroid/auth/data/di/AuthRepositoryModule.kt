package com.gondroid.auth.data.di

import com.gondroid.auth.data.AuthConfigProviderImpl
import com.gondroid.auth.data.AuthRepositoryImpl
import com.gondroid.auth.domain.AuthRepository
import com.gondroid.auth.domain.provider.AuthConfigProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Binds
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    abstract fun bindAuthConfigProvider(
        authConfigProviderImpl: AuthConfigProviderImpl
    ): AuthConfigProvider
}