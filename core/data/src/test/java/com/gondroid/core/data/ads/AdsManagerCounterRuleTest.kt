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
            pdfRewardedId = "test-rewarded-id",
            premiumRepository = FakePremiumRepository(),
            consentManager = FakeAdsConsentManager()
        )
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

    // Consent gating — without UMP consent no ad may even be requested. Outside Robolectric the
    // real InterstitialAd.load would throw, so returning normally proves nothing was requested.

    @Test fun `preloads request no ad until consent allows it`() {
        val noConsentManager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            pdfRewardedId = "test-rewarded-id",
            premiumRepository = FakePremiumRepository(),
            consentManager = FakeAdsConsentManager(canRequestAds = false)
        )
        noConsentManager.preloadPdfRewarded(mockk(relaxed = true))
        noConsentManager.preloadEvaluationInterstitial(mockk(relaxed = true))
    }

    // Premium gating — a premium user must never see an ad, regardless of counters.

    @Test fun `premium user never gets a pdf rewarded ad`() {
        val premiumManager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            pdfRewardedId = "test-rewarded-id",
            premiumRepository = FakePremiumRepository(isPremium = true),
            consentManager = FakeAdsConsentManager()
        )
        // Outside Robolectric RewardedAd.load would throw: returning proves nothing was requested.
        premiumManager.preloadPdfRewarded(mockk(relaxed = true))
        assertThat(premiumManager.isPdfRewardedReady()).isFalse()
        var outcome: RewardedOutcome? = null
        premiumManager.showPdfRewarded(mockk<Activity>(relaxed = true)) { outcome = it }
        assertThat(outcome).isEqualTo(RewardedOutcome.UNAVAILABLE)
    }

    @Test fun `premium user never sees evaluation interstitial regardless of counter`() = runTest {
        val premiumManager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            pdfRewardedId = "test-rewarded-id",
            premiumRepository = FakePremiumRepository(isPremium = true),
            consentManager = FakeAdsConsentManager()
        )
        repeat(6) { premiumManager.recordEvaluationStart() }
        assertThat(premiumManager.shouldShowEvaluationInterstitial()).isFalse()
    }

    @Test fun `showEvaluationInterstitial dismisses immediately for premium user without showing an ad`() {
        val premiumManager = AdsManagerImpl(
            prefs = prefs,
            interstitialId = "test-id",
            pdfRewardedId = "test-rewarded-id",
            premiumRepository = FakePremiumRepository(isPremium = true),
            consentManager = FakeAdsConsentManager()
        )
        var adWasShown: Boolean? = null
        premiumManager.showEvaluationInterstitial(mockk<Activity>(relaxed = true)) {
            adWasShown = it
        }
        assertThat(adWasShown).isFalse()
    }

    // Without a loaded ad the download must go ahead: the caller treats UNAVAILABLE as "download".
    @Test fun `showPdfRewarded reports unavailable when no ad is loaded`() {
        assertThat(manager.isPdfRewardedReady()).isFalse()
        var outcome: RewardedOutcome? = null
        manager.showPdfRewarded(mockk<Activity>(relaxed = true)) { outcome = it }
        assertThat(outcome).isEqualTo(RewardedOutcome.UNAVAILABLE)
    }

    @Test fun `showEvaluationInterstitial reports no ad shown when none is loaded`() {
        var adWasShown: Boolean? = null
        manager.showEvaluationInterstitial(mockk<Activity>(relaxed = true)) { adWasShown = it }
        assertThat(adWasShown).isFalse()
    }
}
