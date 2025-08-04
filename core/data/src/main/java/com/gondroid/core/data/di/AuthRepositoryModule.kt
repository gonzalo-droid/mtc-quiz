package com.gondroid.core.data.di

import com.gondroid.core.data.repository.AuthConfigProviderImpl
import com.gondroid.core.data.repository.AuthRepositoryImpl
import com.gondroid.core.domain.provider.AuthConfigProvider
import com.gondroid.core.domain.repository.AuthRepository
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