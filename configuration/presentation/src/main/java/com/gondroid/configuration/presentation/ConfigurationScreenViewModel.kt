package com.gondroid.configuration.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class ConfigurationScreenViewModel
@Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {


    private var _state = MutableStateFlow(ConfigurationState())
    val state = _state.asStateFlow()

    private var eventChannel = Channel<ConfigurationEvent>()
    val event = eventChannel.receiveAsFlow()

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            eventChannel.send(ConfigurationEvent.Success)
        }
    }
}