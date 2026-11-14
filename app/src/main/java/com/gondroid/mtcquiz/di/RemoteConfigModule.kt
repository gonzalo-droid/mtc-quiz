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
object RemoteConfigModule {

    private const val RELEASE_MIN_FETCH_INTERVAL_SECONDS = 3600L

    @Provides
    @Singleton
    @Named("remoteConfigMinFetchIntervalSeconds")
    fun provideMinFetchIntervalSeconds(): Long =
        if (BuildConfig.DEBUG) 0L else RELEASE_MIN_FETCH_INTERVAL_SECONDS
}
