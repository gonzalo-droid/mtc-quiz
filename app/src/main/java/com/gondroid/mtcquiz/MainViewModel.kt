package com.gondroid.mtcquiz


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.mtcquiz.domain.repository.PreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject


@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    init {
        preferenceRepository.isLoggedInFlow
            .onEach { isLoggedIn ->
                _state.value = AuthState(
                    isLoggedIn = isLoggedIn,
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)
    }
}

data class AuthState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = true
)