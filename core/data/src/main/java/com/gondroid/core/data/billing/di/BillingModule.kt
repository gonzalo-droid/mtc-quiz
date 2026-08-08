package com.gondroid.core.data.billing.di

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.gondroid.core.data.billing.BillingClientFactory
import com.gondroid.core.data.billing.BillingLauncher
import com.gondroid.core.data.billing.PremiumRepositoryImpl
import com.gondroid.core.domain.repository.PremiumRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    companion object {
        @Provides
        @Singleton
        fun provideBillingClientFactory(@ApplicationContext context: Context): BillingClientFactory =
            BillingClientFactory { listener ->
                BillingClient.newBuilder(context)
                    .setListener(listener)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build()
                    )
                    .enableAutoServiceReconnection()
                    .build()
            }
    }
}
