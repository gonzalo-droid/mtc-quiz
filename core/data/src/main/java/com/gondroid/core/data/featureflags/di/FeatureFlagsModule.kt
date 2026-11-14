package com.gondroid.core.data.featureflags.di

import com.gondroid.core.data.featureflags.RemoteConfigFeatureFlagsRepository
import com.gondroid.core.domain.repository.FeatureFlagsRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureFlagsModule {

    @Binds
    @Singleton
    abstract fun bindFeatureFlagsRepository(
        impl: RemoteConfigFeatureFlagsRepository
    ): FeatureFlagsRepository

    companion object {
        /**
         * El intervalo mínimo entre fetches lo decide el módulo `app` (depende de BuildConfig.DEBUG):
         * 0 en debug para probar cambios al instante; en release, una hora para no caer en el
         * throttling de Firebase y que el interruptor aún reaccione el mismo día.
         */
        @Provides
        @Singleton
        fun provideFirebaseRemoteConfig(
            @Named("remoteConfigMinFetchIntervalSeconds") minFetchIntervalSeconds: Long
        ): FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = minFetchIntervalSeconds }
            )
            setDefaultsAsync(RemoteConfigFeatureFlagsRepository.DEFAULTS)
        }
    }
}
