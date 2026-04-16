package com.gondroid.mtcquiz


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.data.billing.BillingManager
import com.gondroid.core.domain.repository.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val preferenceRepository: PreferenceRepository,
    private val billingManager: BillingManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    val isDarkMode = preferenceRepository.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode = preferenceRepository.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val isPremium = billingManager.isPremiumFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isOnboardingShown = preferenceRepository.isOnboardingShownFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun onOnboardingComplete() = viewModelScope.launch {
        preferenceRepository.setIsOnboardingShown(true)
    }

    init {
        combine(
            preferenceRepository.isLoggedInFlow,
            preferenceRepository.isOnboardingShownFlow,
        ) { isLoggedIn, isOnboardingShown ->
            _state.value = AuthState(
                isLoggedIn = isLoggedIn,
                isOnboardingShown = isOnboardingShown,
                isLoading = false,
            )
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            billingManager.refreshPurchaseState()
        }
    }
}

data class AuthState(
    val isLoggedIn: Boolean = false,
    val isOnboardingShown: Boolean = true,
    val isLoading: Boolean = true,
)