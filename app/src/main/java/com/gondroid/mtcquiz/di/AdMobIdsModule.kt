package com.gondroid.mtcquiz.di

import com.gondroid.mtcquiz.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdMobIdsModule {

    @Provides
    @Singleton
    @Named("admobBannerId")
    fun provideBannerId(): String = BuildConfig.ADMOB_BANNER_ID

    @Provides
    @Singleton
    @Named("admobInterstitialId")
    fun provideInterstitialId(): String = BuildConfig.ADMOB_INTERSTITIAL_ID
}
