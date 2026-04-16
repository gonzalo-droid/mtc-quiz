package com.gondroid.configuration.presentation.premium

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.data.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
) : ViewModel() {

    val isPremium = billingManager.isPremiumFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun subscribe(activity: Activity) = viewModelScope.launch {
        _isLoading.value = true
        billingManager.launchSubscription(activity)
        _isLoading.value = false
    }

    fun restorePurchases() = viewModelScope.launch {
        _isLoading.value = true
        billingManager.restorePurchases()
        _isLoading.value = false
    }
}
