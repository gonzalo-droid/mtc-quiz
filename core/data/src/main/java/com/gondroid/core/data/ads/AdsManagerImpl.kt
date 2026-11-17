package com.gondroid.core.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class AdsManagerImpl @Inject constructor(
    private val prefs: AdsPreferences,
    @Named("admobInterstitialId") private val interstitialId: String,
    @Named("admobPdfRewardedId") private val pdfRewardedId: String,
    private val premiumRepository: com.gondroid.core.domain.repository.PremiumRepository,
    private val consentManager: AdsConsentManager
) : AdsManager {

    private val isPremium: Boolean
        get() = premiumRepository.isPremiumFlow.value

    private val canRequestAds: Boolean
        get() = consentManager.canRequestAds.value

    private var pdfRewarded: RewardedAd? = null
    private var isLoadingPdfRewarded: Boolean = false

    private var evaluationInterstitial: InterstitialAd? = null
    private var isLoadingEvaluation: Boolean = false

    override fun preloadPdfRewarded(context: Context) {
        if (isPremium || !canRequestAds) return
        if (pdfRewarded != null || isLoadingPdfRewarded) return
        isLoadingPdfRewarded = true
        RewardedAd.load(
            context,
            pdfRewardedId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    pdfRewarded = ad
                    isLoadingPdfRewarded = false
                }
                override fun onAdFailedToLoad(err: LoadAdError) {
                    pdfRewarded = null
                    isLoadingPdfRewarded = false
                }
            }
        )
    }

    override fun isPdfRewardedReady(): Boolean = !isPremium && pdfRewarded != null

    override fun showPdfRewarded(activity: Activity, onResult: (RewardedOutcome) -> Unit) {
        val ad = pdfRewarded
        if (isPremium || ad == null) {
            onResult(RewardedOutcome.UNAVAILABLE)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                pdfRewarded = null
                preloadPdfRewarded(activity.applicationContext)
                onResult(if (earned) RewardedOutcome.EARNED else RewardedOutcome.SKIPPED)
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                Timber.w("PDF rewarded ad failed to show: %s", err.message)
                pdfRewarded = null
                preloadPdfRewarded(activity.applicationContext)
                onResult(RewardedOutcome.UNAVAILABLE)
            }
        }
        ad.show(activity) { earned = true }
    }

    override fun preloadEvaluationInterstitial(context: Context) {
        if (isPremium || !canRequestAds) return
        if (evaluationInterstitial != null || isLoadingEvaluation) return
        isLoadingEvaluation = true
        InterstitialAd.load(
            context,
            interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    evaluationInterstitial = ad
                    isLoadingEvaluation = false
                }
                override fun onAdFailedToLoad(err: LoadAdError) {
                    evaluationInterstitial = null
                    isLoadingEvaluation = false
                }
            }
        )
    }

    override suspend fun shouldShowEvaluationInterstitial(): Boolean {
        if (isPremium) return false
        val count = prefs.currentEvaluationStartCount()
        return count > 0 && count % 3 == 0
    }

    override fun showEvaluationInterstitial(
        activity: Activity,
        onDismiss: (adWasShown: Boolean) -> Unit
    ) {
        if (isPremium) {
            onDismiss(false)
            return
        }
        val ad = evaluationInterstitial
        if (ad == null) {
            onDismiss(false)
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                evaluationInterstitial = null
                preloadEvaluationInterstitial(activity.applicationContext)
                onDismiss(true)
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                evaluationInterstitial = null
                onDismiss(false)
            }
        }
        ad.show(activity)
    }

    override suspend fun recordEvaluationStart() {
        prefs.incrementEvaluationStartCount()
    }
}
