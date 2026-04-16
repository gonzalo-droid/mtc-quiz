package com.gondroid.pdf.presentation


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.core.data.ads.AdsManager
import com.gondroid.core.domain.repository.QuizRepository
import com.gondroid.core.presentation.ui.PdfScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PdfEvent {
    data object StartDownload : PdfEvent
    data object ShowInterstitial : PdfEvent
}

@HiltViewModel
class PdfScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository,
    val adsManager: AdsManager,
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
    }

    fun loading(isLoading: Boolean) {
        _state.update {
            it.copy(isLoading = isLoading)
        }
    }

    fun onDownloadClicked() = viewModelScope.launch {
        adsManager.recordPdfDownload()
        if (adsManager.shouldShowPdfInterstitial()) {
            eventChannel.send(PdfEvent.ShowInterstitial)
        } else {
            eventChannel.send(PdfEvent.StartDownload)
        }
    }

    fun onInterstitialClosed() = viewModelScope.launch {
        eventChannel.send(PdfEvent.StartDownload)
    }

    fun onDownloadStarting() {
        _state.update { it.copy(shouldDownload = true, isLoading = true) }
    }

    fun onDownloadFinished() {
        _state.update { it.copy(shouldDownload = false, isLoading = false) }
    }

}
