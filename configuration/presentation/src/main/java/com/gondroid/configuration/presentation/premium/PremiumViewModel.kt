package com.gondroid.configuration.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.data.analytics.AnalyticsManager
import com.gondroid.core.data.billing.BillingLauncher
import com.gondroid.core.domain.model.BillingPeriod
import com.gondroid.core.domain.model.SubscriptionPlan
import com.gondroid.core.domain.repository.PremiumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val premiumRepository: PremiumRepository,
    private val billingLauncher: BillingLauncher,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    val isPremium = premiumRepository.isPremiumFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val availablePlans = premiumRepository.availablePlansFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedPlan = MutableStateFlow<SubscriptionPlan?>(null)
    val selectedPlan: StateFlow<SubscriptionPlan?> = _selectedPlan.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _restoreMessage = MutableStateFlow<String?>(null)
    val restoreMessage = _restoreMessage.asStateFlow()

    init {
        analyticsManager.logPaywallViewed()
        viewModelScope.launch {
            premiumRepository.loadAvailablePlans()
        }
        viewModelScope.launch {
            premiumRepository.availablePlansFlow.collect { plans ->
                if (_selectedPlan.value == null) {
                    _selectedPlan.value = plans.firstOrNull { it.billingPeriod == BillingPeriod.ANNUAL }
                        ?: plans.firstOrNull()
                }
            }
        }
    }

    fun selectPlan(plan: SubscriptionPlan) {
        _selectedPlan.value = plan
    }

    fun subscribe(activity: Activity) = viewModelScope.launch {
        val productId = _selectedPlan.value?.productId ?: return@launch
        _isLoading.value = true
        billingLauncher.launchSubscription(activity, productId)
        _isLoading.value = false
    }

    fun restorePurchases() = viewModelScope.launch {
        _isLoading.value = true
        premiumRepository.restorePurchases()
        _isLoading.value = false
        val restored = premiumRepository.isPremiumFlow.first()
        _restoreMessage.value = if (restored) {
            "Compra restaurada correctamente"
        } else {
            "No se encontró ninguna suscripción activa"
        }
    }

    fun clearRestoreMessage() {
        _restoreMessage.value = null
    }
}
