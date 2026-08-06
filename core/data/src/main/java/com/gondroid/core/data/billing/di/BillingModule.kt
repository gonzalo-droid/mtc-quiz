package com.gondroid.core.data.billing.di

import com.gondroid.core.data.billing.BillingLauncher
import com.gondroid.core.data.billing.PremiumRepositoryImpl
import com.gondroid.core.domain.repository.PremiumRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BillingModule {

    @Binds
    @Singleton
    abstract fun bindPremiumRepository(impl: PremiumRepositoryImpl): PremiumRepository

    @Binds
    @Singleton
    abstract fun bindBillingLauncher(impl: PremiumRepositoryImpl): BillingLauncher
}
