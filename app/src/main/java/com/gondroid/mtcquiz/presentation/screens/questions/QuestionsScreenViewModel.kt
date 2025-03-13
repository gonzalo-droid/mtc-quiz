package com.gondroid.mtcquiz.presentation.screens.questions


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class QuestionsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {

    var state by mutableStateOf(QuestionsDataState())
        private set


    init {
        viewModelScope.launch {
            repository.getQuestionsByCategory("categoryId")
                .collect { questions ->
                    state = state.copy(questions = questions)
                }
        }
    }
}