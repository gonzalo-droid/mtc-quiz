package com.gondroid.core.data.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AdsManagerImpl @Inject constructor(
    private val prefs: AdsPreferences,
    @Named("admobInterstitialId") private val interstitialId: String,
) : AdsManager {

    private var interstitial: InterstitialAd? = null
    private var isLoading: Boolean = false

    private var evaluationInterstitial: InterstitialAd? = null
    private var isLoadingEvaluation: Boolean = false

    override fun preloadPdfInterstitial(context: Context) {
        if (interstitial != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            context,
            interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(err: LoadAdError) {
                    interstitial = null
                    isLoading = false
                }
            }
        )
    }

    override suspend fun shouldShowPdfInterstitial(): Boolean {
        val count = prefs.currentPdfDownloadCount()
        return count > 0 && count % 3 == 0
    }

    override fun showPdfInterstitial(activity: Activity, onDismiss: () -> Unit) {
        val ad = interstitial
        if (ad == null) {
            onDismiss()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitial = null
                preloadPdfInterstitial(activity.applicationContext)
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                interstitial = null
                onDismiss()
            }
        }
        ad.show(activity)
    }

    override suspend fun recordPdfDownload() {
        prefs.incrementPdfDownloadCount()
    }

    override fun preloadEvaluationInterstitial(context: Context) {
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
        val count = prefs.currentEvaluationStartCount()
        return count > 0 && count % 3 == 0
    }

    override fun showEvaluationInterstitial(activity: Activity, onDismiss: () -> Unit) {
        val ad = evaluationInterstitial
        if (ad == null) {
            onDismiss()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                evaluationInterstitial = null
                preloadEvaluationInterstitial(activity.applicationContext)
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(err: AdError) {
                evaluationInterstitial = null
                onDismiss()
            }
        }
        ad.show(activity)
    }

    override suspend fun recordEvaluationStart() {
        prefs.incrementEvaluationStartCount()
    }
}
