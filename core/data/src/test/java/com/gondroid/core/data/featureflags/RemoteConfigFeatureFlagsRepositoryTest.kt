package com.gondroid.core.data.featureflags

import com.gondroid.core.data.featureflags.RemoteConfigFeatureFlagsRepository.Companion.KEY_PREMIUM_ENABLED
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoteConfigFeatureFlagsRepositoryTest {

    private fun value(source: Int, asBoolean: Boolean = false): FirebaseRemoteConfigValue = mockk {
        every { this@mockk.source } returns source
        every { asBoolean() } returns asBoolean
    }

    private fun completedTask(error: Exception? = null): Task<Boolean> = mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns error
        every { result } returns (error == null)
    }

    private val remoteConfig: FirebaseRemoteConfig = mockk()

    @Test
    fun `falls back to the local default true while Remote Config only has the static value`() {
        // setDefaultsAsync todavía no terminó: Remote Config devolvería false para un booleano.
        every { remoteConfig.getValue(KEY_PREMIUM_ENABLED) } returns
            value(FirebaseRemoteConfig.VALUE_SOURCE_STATIC, asBoolean = false)

        val repository = RemoteConfigFeatureFlagsRepository(remoteConfig)

        assertThat(repository.isPremiumOfferEnabled.value).isTrue()
    }

    @Test
    fun `starts with the last activated remote value`() {
        every { remoteConfig.getValue(KEY_PREMIUM_ENABLED) } returns
            value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asBoolean = false)

        val repository = RemoteConfigFeatureFlagsRepository(remoteConfig)

        assertThat(repository.isPremiumOfferEnabled.value).isFalse()
    }

    @Test
    fun `refresh activates the fetched value and updates the flag`() = runTest {
        every { remoteConfig.getValue(KEY_PREMIUM_ENABLED) } returns
            value(FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT, asBoolean = true)
        val repository = RemoteConfigFeatureFlagsRepository(remoteConfig)
        assertThat(repository.isPremiumOfferEnabled.value).isTrue()

        every { remoteConfig.fetchAndActivate() } returns completedTask()
        every { remoteConfig.getValue(KEY_PREMIUM_ENABLED) } returns
            value(FirebaseRemoteConfig.VALUE_SOURCE_REMOTE, asBoolean = false)
        repository.refresh()

        assertThat(repository.isPremiumOfferEnabled.value).isFalse()
    }

    @Test
    fun `refresh swallows a fetch failure and keeps the current value`() = runTest {
        every { remoteConfig.getValue(KEY_PREMIUM_ENABLED) } returns
            value(FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT, asBoolean = true)
        every { remoteConfig.fetchAndActivate() } returns
            completedTask(IllegalStateException("offline"))
        val repository = RemoteConfigFeatureFlagsRepository(remoteConfig)

        repository.refresh()

        assertThat(repository.isPremiumOfferEnabled.value).isTrue()
    }
}
