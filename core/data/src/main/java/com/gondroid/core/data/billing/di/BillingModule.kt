package com.gondroid.core.data.billing.di

import com.gondroid.core.data.billing.BillingManager
import com.gondroid.core.data.billing.BillingManagerImpl
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
    abstract fun bindBillingManager(impl: BillingManagerImpl): BillingManager
}
