package com.gondroid.presentation.screens.questions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.gondroid.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestionsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: QuizRepository
) : ViewModel() {

    private var _state = MutableStateFlow(QuestionsDataState())
    val state = _state.asStateFlow()

    private val data = savedStateHandle.toRoute<QuestionsScreenRoute>()


    init {
        data.categoryId.let {
            viewModelScope.launch {
                repository.getCategoryById(it)?.let { category ->
                    _state.update {
                        it.copy(category = category)
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.getQuestionsByCategory("categoryId")
                .collect { questions ->
                    _state.update {
                        it.copy(questions = questions)
                    }
                }
        }
    }
}