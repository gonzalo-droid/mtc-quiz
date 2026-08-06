package com.gondroid.core.data.analytics

interface AnalyticsManager {
    fun logPaywallViewed()
    fun logSubscribeClicked(productId: String)
    fun logPurchaseCompleted(productId: String)
    fun logPurchaseCanceled(productId: String)
    fun logPurchaseFailed(productId: String, errorCode: Int)
    fun logRestoreClicked()
    fun logRestoreCompleted(isPremium: Boolean)
}
