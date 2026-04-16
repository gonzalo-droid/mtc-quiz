package com.gondroid.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.data.billing.BillingManager
import com.gondroid.core.domain.repository.PreferenceRepository
import com.gondroid.core.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Named


@HiltViewModel
class HomeScreenViewModel
@Inject
constructor(
    private val repository: QuizRepository,
    private val preferenceRepository: PreferenceRepository,
    private val billingManager: BillingManager,
    @Named("admobBannerId") val bannerAdId: String,
) : ViewModel() {

    private var _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        repository.categoriesFlow.onEach { categories ->
            _state.value = _state.value.copy(categories = categories)
        }.launchIn(viewModelScope)

        preferenceRepository.userNameFlow.onEach { userName ->
            _state.update {
                it.copy(userName = userName)
            }
        }.launchIn(viewModelScope)

        preferenceRepository.currentStreakFlow.onEach { streak ->
            _state.update {
                it.copy(streak = streak)
            }
        }.launchIn(viewModelScope)

        billingManager.isPremiumFlow.onEach { isPremium ->
            _state.update {
                it.copy(isPremium = isPremium)
            }
        }.launchIn(viewModelScope)
    }
}