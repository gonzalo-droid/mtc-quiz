package com.gondroid.evaluation.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gondroid.core.domain.model.EvaluationState
import com.gondroid.core.domain.repository.QuizRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: QuizRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(StatsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllEvaluations().collect { evaluations ->
                val totalApproved = evaluations.count { it.state == EvaluationState.APPROVED }
                val totalRejected = evaluations.count { it.state == EvaluationState.REJECTED }
                val approvalRate = if (evaluations.isNotEmpty()) totalApproved.toFloat() / evaluations.size else 0f
                val totalQuestions = evaluations.sumOf { it.totalQuestions }
                val totalCorrect = evaluations.sumOf { it.totalCorrect }

                val categoryStats = evaluations
                    .groupBy { it.categoryTitle }
                    .map { (title, evals) ->
                        CategoryStat(
                            categoryTitle = title,
                            evaluationCount = evals.size,
                            approvalRate = evals.count { it.state == EvaluationState.APPROVED }.toFloat() / evals.size,
                        )
                    }
                    .sortedBy { it.approvalRate }

                _state.update {
                    it.copy(
                        totalEvaluations = evaluations.size,
                        totalApproved = totalApproved,
                        totalRejected = totalRejected,
                        approvalRate = approvalRate,
                        totalQuestionsAnswered = totalQuestions,
                        totalCorrectAnswers = totalCorrect,
                        categoryStats = categoryStats,
                        isLoading = false,
                    )
                }
            }
        }
    }
}
