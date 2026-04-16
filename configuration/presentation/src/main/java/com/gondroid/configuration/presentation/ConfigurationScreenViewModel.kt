package com.gondroid.configuration.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.domain.repository.AuthRepository
import com.gondroid.core.domain.repository.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ConfigurationScreenViewModel
@Inject constructor(
    private val repository: AuthRepository,
    private val preferenceRepository: PreferenceRepository,
) : ViewModel() {

    private var _state = MutableStateFlow(ConfigurationState())
    val state = _state.asStateFlow()

    private var eventChannel = Channel<ConfigurationEvent>()
    val event = eventChannel.receiveAsFlow()

    init {
        preferenceRepository.darkModeFlow
            .onEach { isDarkMode ->
                _state.update { it.copy(isDarkMode = isDarkMode) }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: ConfigurationAction) {
        when (action) {
            is ConfigurationAction.ToggleDarkMode -> {
                viewModelScope.launch {
                    preferenceRepository.setDarkMode(action.enabled)
                }
            }
            else -> Unit
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            eventChannel.send(ConfigurationEvent.Success)
        }
    }
}