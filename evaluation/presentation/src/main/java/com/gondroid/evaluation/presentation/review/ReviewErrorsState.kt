package com.gondroid.evaluation.presentation.review

data class ReviewErrorsState(
    val frequentErrors: List<FrequentError> = emptyList(),
    val isLoading: Boolean = true,
)

data class FrequentError(
    val questionId: Int,
    val question: String,
    val failCount: Int,
    val lastWrongAnswer: String,
    val correctAnswer: String,
)
