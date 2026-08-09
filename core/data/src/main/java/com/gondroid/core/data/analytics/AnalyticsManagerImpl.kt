package com.gondroid.core.data.analytics

import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManagerImpl @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) : AnalyticsManager {

    override fun logPaywallViewed() {
        firebaseAnalytics.logEvent("paywall_viewed", null)
    }

    override fun logSubscribeClicked(productId: String) {
        firebaseAnalytics.logEvent(
            "premium_subscribe_clicked",
            bundleOf("product_id" to productId)
        )
    }

    override fun logPurchaseCompleted(productId: String) {
        firebaseAnalytics.logEvent(
            "premium_purchase_completed",
            bundleOf("product_id" to productId)
        )
    }

    override fun logPurchaseCanceled(productId: String) {
        firebaseAnalytics.logEvent(
            "premium_purchase_canceled",
            bundleOf("product_id" to productId)
        )
    }

    override fun logPurchaseFailed(productId: String, errorCode: Int) {
        firebaseAnalytics.logEvent(
            "premium_purchase_failed",
            bundleOf("product_id" to productId, "error_code" to errorCode)
        )
    }

    override fun logRestoreClicked() {
        firebaseAnalytics.logEvent("premium_restore_clicked", null)
    }

    override fun logRestoreCompleted(isPremium: Boolean) {
        firebaseAnalytics.logEvent(
            "premium_restore_completed",
            bundleOf("is_premium" to isPremium)
        )
    }
}
