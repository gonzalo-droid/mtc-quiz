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
}
