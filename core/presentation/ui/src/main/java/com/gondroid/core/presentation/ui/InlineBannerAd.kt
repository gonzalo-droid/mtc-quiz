package com.gondroid.core.presentation.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * The banners of one list screen, one per slot. A lazy list disposes an item as it scrolls off
 * screen; keeping each [AdView] here means scrolling back shows the same ad instead of requesting
 * a new one every time. Create it with [rememberInlineBannerAds], which destroys them all when
 * the screen leaves composition.
 */
class InlineBannerAds internal constructor(
    private val context: Context,
    private val adUnitId: String,
    val adSize: AdSize
) {
    private val adViews = mutableMapOf<Int, AdView>()
    private val requested = mutableSetOf<Int>()

    internal fun adViewFor(slot: Int): AdView = adViews.getOrPut(slot) {
        AdView(context).apply {
            adUnitId = this@InlineBannerAds.adUnitId
            setAdSize(this@InlineBannerAds.adSize)
        }
    }

    /** Requests the ad of [slot] once; later calls for the same slot do nothing. */
    internal fun loadOnce(slot: Int) {
        if (requested.add(slot)) adViewFor(slot).loadAd(AdRequest.Builder().build())
    }

    internal fun destroyAll() {
        adViews.values.forEach { it.destroy() }
        adViews.clear()
        requested.clear()
    }
}

/**
 * [horizontalInset] is the list's total side padding: the banner is sized to the width it really
 * gets, or its right edge would be clipped.
 */
@Composable
fun rememberInlineBannerAds(adUnitId: String, horizontalInset: Dp = 0.dp): InlineBannerAds {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp - horizontalInset.value.toInt()
    val banners = remember(adUnitId, widthDp) {
        InlineBannerAds(
            context = context,
            adUnitId = adUnitId,
            // Anchored-adaptive size: a fixed height known before loading, so the list does not
            // jump when the ad arrives.
            adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        )
    }
    DisposableEffect(banners) {
        onDispose { banners.destroyAll() }
    }
    return banners
}

/**
 * One banner between the items of a list. Reserves the banner's height up front and requests
 * its ad only once [LocalCanRequestAds] allows it. Callers decide whether a premium user gets a
 * slot at all.
 */
@Composable
fun InlineBannerAdSlot(banners: InlineBannerAds, slot: Int, modifier: Modifier = Modifier) {
    if (!LocalAdsEnabled.current) return
    val isInspectionMode = LocalInspectionMode.current
    val canRequestAds = LocalCanRequestAds.current
    LaunchedEffect(slot, canRequestAds) {
        if (!isInspectionMode && canRequestAds) banners.loadOnce(slot)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(banners.adSize.height.dp),
        contentAlignment = Alignment.Center
    ) {
        BannerAdView(adView = banners.adViewFor(slot))
    }
}
