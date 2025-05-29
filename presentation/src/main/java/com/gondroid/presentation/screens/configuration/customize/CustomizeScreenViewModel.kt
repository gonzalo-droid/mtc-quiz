package com.gondroid.presentation.screens.configuration.customize


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.domain.models.PreferencesEvaluation
import com.gondroid.domain.repository.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CustomizeScreenViewModel
@Inject
constructor(
    private val repository: PreferenceRepository
) : ViewModel() {

    private var _state = MutableStateFlow(CustomizeDataState())
    val state = _state.asStateFlow()

    private var eventChannel = Channel<CustomizeEvent>()
    val event = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.getInitEvaluation().let { data ->
                _state.update {
                    it.copy(
                        numberQuestions = data.numberQuestions,
                        timeToFinishEvaluation = data.timeToFinishEvaluation,
                        percentageToApprovedEvaluation = data.percentageToApprovedEvaluation
                    )
                }
            }
        }

    }

    fun updateValues(
        percentageToApprovedEvaluation: String,
        timeToFinishEvaluation: String,
        numberQuestions: String
    ) {
        _state.update {
            it.copy(
                numberQuestions = numberQuestions,
                timeToFinishEvaluation = timeToFinishEvaluation,
                percentageToApprovedEvaluation = percentageToApprovedEvaluation
            )
        }
        viewModelScope.launch {
            val result = repository.setPreferencesEvaluation(
                PreferencesEvaluation(
                    numberQuestions = numberQuestions,
                    timeToFinishEvaluation = timeToFinishEvaluation,
                    percentageToApprovedEvaluation = percentageToApprovedEvaluation
                )
            )
            if(result) eventChannel.send(CustomizeEvent.Success)
            else eventChannel.send(CustomizeEvent.Error)
        }
    }
}