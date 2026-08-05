package com.gondroid.core.presentation.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Creates, loads, and tears down a banner [AdView] for [bannerAdId], and
 * renders it centered at the bottom of whatever content this is placed in.
 * Renders nothing when [isPremium] is true.
 */
@Composable
fun BannerAdSlot(bannerAdId: String, isPremium: Boolean, modifier: Modifier = Modifier) {
    if (isPremium) return

    val context = LocalContext.current
    val adView = remember {
        AdView(context).apply {
            adUnitId = bannerAdId
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, 360)
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d("adMobTest", "Banner ad was loaded.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("adMobTest", "Banner ad failed to load: ${error.message}")
                }

                override fun onAdImpression() {
                    Log.d("adMobTest", "Banner ad recorded an impression.")
                }

                override fun onAdClicked() {
                    Log.d("adMobTest", "Banner ad was clicked.")
                }
            }
        }
    }

    val isInspectionMode = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (!isInspectionMode) {
            adView.loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(Unit) {
        onDispose { adView.destroy() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BannerAdView(adView)
    }
}
