package com.gondroid.pdf.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.data.ads.RewardedOutcome
import com.gondroid.core.domain.repository.FeatureFlagsRepository
import com.gondroid.core.domain.repository.QuizRepository
import com.gondroid.core.presentation.ui.PdfScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface PdfEvent {
    data object StartDownload : PdfEvent
    data object ShowRewarded : PdfEvent
    data object RewardSkipped : PdfEvent
}

@HiltViewModel
class PdfScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
    val adsManager: AdsManager,
    private val featureFlagsRepository: FeatureFlagsRepository
) : ViewModel() {

    private var _state = MutableStateFlow(PdfState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<PdfEvent>()
    val events: Flow<PdfEvent> = eventChannel.receiveAsFlow()

    private val data = savedStateHandle.toRoute<PdfScreenRoute>()

    init {
        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    _state.update {
                        it.copy(category = category, isLoading = true)
                    }
                }
            }
        }

        featureFlagsRepository.isPremiumOfferEnabled.onEach { enabled ->
            _state.update { it.copy(isPremiumOfferEnabled = enabled) }
        }.launchIn(viewModelScope)
    }

    fun loading(isLoading: Boolean) {
        _state.update {
            it.copy(isLoading = isLoading)
        }
    }

    /**
     * Cada descarga pasa por el anuncio con recompensa cuando hay uno listo. Si no lo hay
     * (premium, sin consentimiento, sin red o sin inventario) se descarga directo: un PDF
     * nunca queda bloqueado por falta de anuncio.
     */
    fun onDownloadClicked() = viewModelScope.launch {
        if (adsManager.isPdfRewardedReady()) {
            _state.update { it.copy(showRewardDialog = true) }
        } else {
            eventChannel.send(PdfEvent.StartDownload)
        }
    }

    fun onWatchRewardedClicked() = viewModelScope.launch {
        _state.update { it.copy(showRewardDialog = false) }
        eventChannel.send(PdfEvent.ShowRewarded)
    }

    fun onRewardDialogDismissed() {
        _state.update { it.copy(showRewardDialog = false) }
    }

    fun onRewardedResult(outcome: RewardedOutcome) = viewModelScope.launch {
        Timber.d("PDF rewarded ad finished: %s", outcome)
        when (outcome) {
            // UNAVAILABLE: el anuncio falló al mostrarse, el usuario no tiene la culpa.
            RewardedOutcome.EARNED, RewardedOutcome.UNAVAILABLE ->
                eventChannel.send(PdfEvent.StartDownload)
            RewardedOutcome.SKIPPED -> eventChannel.send(PdfEvent.RewardSkipped)
        }
    }

    fun onDownloadStarting() {
        _state.update { it.copy(shouldDownload = true, isLoading = true) }
    }

    fun onDownloadFinished() {
        _state.update { it.copy(shouldDownload = false, isLoading = false) }
    }
}
