package com.gondroid.core.data.analytics.di

import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.data.analytics.AnalyticsManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(impl: AnalyticsManagerImpl): AnalyticsManager
}
