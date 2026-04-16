package com.gondroid.detail.presentation


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.domain.repository.QuizRepository
import com.gondroid.core.presentation.ui.DetailScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailEvent {
    data class NavigateToEvaluation(val categoryId: String) : DetailEvent
    data object ShowEvaluationInterstitial : DetailEvent
}

@HiltViewModel
class DetailScreenViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
    val adsManager: AdsManager,
) : ViewModel() {

    private var _state = MutableStateFlow(DetailState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<DetailEvent>()
    val events: Flow<DetailEvent> = eventChannel.receiveAsFlow()

    private val data = savedStateHandle.toRoute<DetailScreenRoute>()

    init {
        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    _state.update {
                        it.copy(category = category)
                    }
                }
            }
        }
    }

    fun onStartEvaluation(categoryId: String) = viewModelScope.launch {
        adsManager.recordEvaluationStart()
        if (adsManager.shouldShowEvaluationInterstitial()) {
            eventChannel.send(DetailEvent.ShowEvaluationInterstitial)
        } else {
            eventChannel.send(DetailEvent.NavigateToEvaluation(categoryId))
        }
    }

    fun onInterstitialClosed(categoryId: String) = viewModelScope.launch {
        eventChannel.send(DetailEvent.NavigateToEvaluation(categoryId))
    }
}