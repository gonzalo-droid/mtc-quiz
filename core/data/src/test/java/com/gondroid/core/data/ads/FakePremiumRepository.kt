package com.gondroid.core.data.ads

import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakePremiumRepository(isPremium: Boolean = false) : PremiumRepository {
    override val isPremiumFlow: StateFlow<Boolean> = MutableStateFlow(isPremium)
    override val availablePlansFlow: Flow<List<SubscriptionPlan>> = MutableStateFlow(emptyList())
    override suspend fun loadAvailablePlans() {}
    override suspend fun refreshPurchaseState() {}
    override suspend fun restorePurchases() {}
}
