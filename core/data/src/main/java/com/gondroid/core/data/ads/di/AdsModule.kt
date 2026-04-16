package com.gondroid.core.data.ads.di

import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.data.ads.AdsManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {

    @Binds
    @Singleton
    abstract fun bindAdsManager(impl: AdsManagerImpl): AdsManager
}
