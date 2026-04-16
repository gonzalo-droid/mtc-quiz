package com.gondroid.core.data.ads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AdsManagerCounterRuleTest {

    private lateinit var prefs: FakeAdsPreferences
    private lateinit var manager: AdsManagerImpl

    @Before fun setUp() {
        prefs = FakeAdsPreferences()
        manager = AdsManagerImpl(prefs = prefs, interstitialId = "test-id")
    }

    @Test fun `first download does not show interstitial`() = runTest {
        manager.recordPdfDownload()
        assertThat(manager.shouldShowPdfInterstitial()).isFalse()
    }

    @Test fun `second download does not show interstitial`() = runTest {
        repeat(2) { manager.recordPdfDownload() }
        assertThat(manager.shouldShowPdfInterstitial()).isFalse()
    }

    @Test fun `third download shows interstitial`() = runTest {
        repeat(3) { manager.recordPdfDownload() }
        assertThat(manager.shouldShowPdfInterstitial()).isTrue()
    }

    @Test fun `sixth download shows interstitial`() = runTest {
        repeat(6) { manager.recordPdfDownload() }
        assertThat(manager.shouldShowPdfInterstitial()).isTrue()
    }

    @Test fun `counter 0 never shows interstitial`() = runTest {
        assertThat(manager.shouldShowPdfInterstitial()).isFalse()
    }
}
