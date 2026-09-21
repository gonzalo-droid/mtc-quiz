package com.gondroid.core.data.ads

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AdsManagerCounterRuleTest {

    private lateinit var prefs: FakeAdsPreferences
    private lateinit var manager: AdsManagerImpl

    @Before fun setUp() {
        prefs = FakeAdsPreferences()
        manager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            premiumRepository = FakePremiumRepository()
        )
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

    // Evaluation counter tests

    @Test fun `first evaluation does not show interstitial`() = runTest {
        manager.recordEvaluationStart()
        assertThat(manager.shouldShowEvaluationInterstitial()).isFalse()
    }

    @Test fun `second evaluation does not show interstitial`() = runTest {
        repeat(2) { manager.recordEvaluationStart() }
        assertThat(manager.shouldShowEvaluationInterstitial()).isFalse()
    }

    @Test fun `third evaluation shows interstitial`() = runTest {
        repeat(3) { manager.recordEvaluationStart() }
        assertThat(manager.shouldShowEvaluationInterstitial()).isTrue()
    }

    @Test fun `sixth evaluation shows interstitial`() = runTest {
        repeat(6) { manager.recordEvaluationStart() }
        assertThat(manager.shouldShowEvaluationInterstitial()).isTrue()
    }

    @Test fun `evaluation counter 0 never shows interstitial`() = runTest {
        assertThat(manager.shouldShowEvaluationInterstitial()).isFalse()
    }

    @Test fun `pdf and evaluation counters are independent`() = runTest {
        repeat(3) { manager.recordPdfDownload() }
        assertThat(manager.shouldShowEvaluationInterstitial()).isFalse()
        repeat(3) { manager.recordEvaluationStart() }
        assertThat(manager.shouldShowPdfInterstitial()).isTrue()
        assertThat(manager.shouldShowEvaluationInterstitial()).isTrue()
    }

    // Premium gating — a premium user must never see an ad, regardless of counters.

    @Test fun `premium user never sees pdf interstitial regardless of counter`() = runTest {
        val premiumManager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            premiumRepository = FakePremiumRepository(isPremium = true)
        )
        repeat(6) { premiumManager.recordPdfDownload() }
        assertThat(premiumManager.shouldShowPdfInterstitial()).isFalse()
    }

    @Test fun `premium user never sees evaluation interstitial regardless of counter`() = runTest {
        val premiumManager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            premiumRepository = FakePremiumRepository(isPremium = true)
        )
        repeat(6) { premiumManager.recordEvaluationStart() }
        assertThat(premiumManager.shouldShowEvaluationInterstitial()).isFalse()
    }

    @Test fun `showPdfInterstitial dismisses immediately for premium user without showing an ad`() {
        val premiumManager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            premiumRepository = FakePremiumRepository(isPremium = true)
        )
        var adWasShown: Boolean? = null
        premiumManager.showPdfInterstitial(mockk<Activity>(relaxed = true)) { adWasShown = it }
        assertThat(adWasShown).isFalse()
    }

    @Test fun `showEvaluationInterstitial dismisses immediately for premium user without showing an ad`() {
        val premiumManager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            premiumRepository = FakePremiumRepository(isPremium = true)
        )
        var adWasShown: Boolean? = null
        premiumManager.showEvaluationInterstitial(mockk<Activity>(relaxed = true)) { adWasShown = it }
        assertThat(adWasShown).isFalse()
    }

    // The flag is what decides whether "¿Cansado de los anuncios?" is offered: with no ad loaded
    // the callback still runs (the caller has to proceed), but it must not claim an ad was shown.
    @Test fun `showPdfInterstitial reports no ad shown when none is loaded`() {
        var adWasShown: Boolean? = null
        manager.showPdfInterstitial(mockk<Activity>(relaxed = true)) { adWasShown = it }
        assertThat(adWasShown).isFalse()
    }

    @Test fun `showEvaluationInterstitial reports no ad shown when none is loaded`() {
        var adWasShown: Boolean? = null
        manager.showEvaluationInterstitial(mockk<Activity>(relaxed = true)) { adWasShown = it }
        assertThat(adWasShown).isFalse()
    }
}
