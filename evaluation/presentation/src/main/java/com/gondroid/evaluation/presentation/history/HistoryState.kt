package com.gondroid.evaluation.presentation.history

import com.gondroid.core.domain.model.Evaluation

data class HistoryState(
    val evaluations: List<Evaluation> = emptyList(),
    val isLoading: Boolean = true,
)
