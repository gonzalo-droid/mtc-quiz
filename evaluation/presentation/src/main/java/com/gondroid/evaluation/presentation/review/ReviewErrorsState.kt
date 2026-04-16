package com.gondroid.evaluation.presentation.review

import com.gondroid.core.domain.model.QuestionResult

data class ReviewErrorsState(
    val failedQuestions: List<QuestionResult> = emptyList(),
    val isLoading: Boolean = true,
)
