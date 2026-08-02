package com.gondroid.core.data.ads

import android.app.Activity
import com.gondroid.core.data.billing.BillingManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeBillingManager(isPremium: Boolean = false) : BillingManager {
    override val isPremiumFlow: Flow<Boolean> = MutableStateFlow(isPremium)
    override suspend fun launchSubscription(activity: Activity): Boolean = false
    override suspend fun refreshPurchaseState() {}
    override suspend fun restorePurchases() {}
}
