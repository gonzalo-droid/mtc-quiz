package com.gondroid.evaluation.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.database.dao.DismissedQuestionDao
import com.gondroid.core.database.entity.DismissedQuestionEntity
import com.gondroid.core.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewErrorsViewModel @Inject constructor(
    private val repository: QuizRepository,
    private val dismissedDao: DismissedQuestionDao,
) : ViewModel() {

    private val _state = MutableStateFlow(ReviewErrorsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllEvaluations(),
                dismissedDao.getAllDismissedIds(),
            ) { evaluations, dismissedIds ->
                val allFailedResults = evaluations
                    .flatMap { it.questionResults }
                    .filter { !it.isCorrect }

                allFailedResults
                    .groupBy { it.questionId }
                    .filter { (questionId, results) ->
                        results.size >= 3 && questionId !in dismissedIds
                    }
                    .map { (questionId, results) ->
                        val latest = results.last()
                        FrequentError(
                            questionId = questionId,
                            question = latest.question,
                            failCount = results.size,
                            lastWrongAnswer = latest.option ?: "",
                            correctAnswer = latest.correctAnswer,
                        )
                    }
                    .sortedByDescending { it.failCount }
            }.collect { frequentErrors ->
                _state.update {
                    it.copy(
                        frequentErrors = frequentErrors,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun dismissQuestion(questionId: Int) = viewModelScope.launch {
        dismissedDao.dismiss(DismissedQuestionEntity(questionId))
    }

    fun restoreAllDismissed() = viewModelScope.launch {
        dismissedDao.restoreAll()
    }
}
