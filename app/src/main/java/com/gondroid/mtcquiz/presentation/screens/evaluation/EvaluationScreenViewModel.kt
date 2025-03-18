package com.gondroid.mtcquiz.presentation.screens.evaluation


import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class EvaluationScreenViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {

    var state by mutableStateOf(EvaluationDataState())
        private set

    private val _indexQuestion = MutableStateFlow<Int>(0)
    val indexQuestion: StateFlow<Int> = _indexQuestion


    init {
        state = state.copy(indexQuestion = indexQuestion.value)
        viewModelScope.launch {
            repository.getQuestionsByCategory("categoryId")
                .collect { questions ->
                    state =
                        state.copy(questions = questions, question = questions[indexQuestion.value])
                }
        }
    }

    fun nextQuestion() {
        _indexQuestion.value = indexQuestion.value.inc()
        Log.d("EvaluationScreenViewModel", "nextQuestion: ${indexQuestion.value}")
        state = state.copy(
            indexQuestion = indexQuestion.value,
            question = state.questions[indexQuestion.value]
        )
    }
}