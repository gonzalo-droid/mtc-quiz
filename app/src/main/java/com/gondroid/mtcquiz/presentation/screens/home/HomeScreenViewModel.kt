package com.gondroid.mtcquiz.presentation.screens.home


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.mtcquiz.domain.repository.PreferenceRepository
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject


@HiltViewModel
class HomeScreenViewModel
@Inject
constructor(
    private val repository: QuizRepository,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    var state by mutableStateOf(HomeDataState())
        private set

    init {
        repository.categoriesFlow.onEach { categories ->
            state = state.copy(categories = categories)
        }.launchIn(viewModelScope)

        preferenceRepository.userNameFlow.onEach { userName ->
            state = state.copy(userName = userName)
        }.launchIn(viewModelScope)

    }
}