package com.gondroid.mtcquiz.presentation.screens.questions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import com.gondroid.mtcquiz.presentation.navigation.QuestionsScreenRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {

    var state by mutableStateOf(QuestionsDataState())
        private set

    private val data = savedStateHandle.toRoute<QuestionsScreenRoute>()


    init {
        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    state = state.copy(category = category)
                }

            }
        }

        viewModelScope.launch {
            repository.getQuestionsByCategory("categoryId")
                .collect { questions ->
                    state = state.copy(questions = questions)
                }
        }
    }
}