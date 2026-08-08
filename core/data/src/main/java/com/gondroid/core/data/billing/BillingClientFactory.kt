package com.gondroid.core.data.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

/**
 * Indirection around [BillingClient.newBuilder] so [PremiumRepositoryImpl] can be unit-tested
 * with a fake [BillingClient] instead of the real Play Billing client, which requires a live
 * Play Store connection and can't be constructed in a plain JVM/Robolectric test.
 */
fun interface BillingClientFactory {
    fun create(listener: PurchasesUpdatedListener): BillingClient
}
