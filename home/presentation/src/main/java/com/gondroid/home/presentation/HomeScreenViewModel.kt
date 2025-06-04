package com.gondroid.home.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.domain.repository.PreferenceRepository
import com.gondroid.domain.repository.QuizRepository
import com.gondroid.mtcquiz.domain.repository.PreferenceRepository
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel
@Inject
constructor(
    private val repository: QuizRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private var _state = MutableStateFlow(HomeDataState())
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
    }
}