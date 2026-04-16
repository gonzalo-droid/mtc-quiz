package com.gondroid.evaluation.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewErrorsViewModel @Inject constructor(
    private val repository: QuizRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewErrorsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllEvaluations().collect { evaluations ->
                val failedQuestions = evaluations
                    .flatMap { it.questionResults }
                    .filter { !it.isCorrect }
                    .distinctBy { it.questionId }
                _state.update {
                    it.copy(
                        failedQuestions = failedQuestions,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
