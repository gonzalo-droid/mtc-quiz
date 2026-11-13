package com.gondroid.evaluation.presentation.review

data class ReviewErrorsState(
    val frequentErrors: List<FrequentError> = emptyList(),
    val isLoading: Boolean = true
)

data class FrequentError(
    /** [errorKey] of the question: what the list is keyed by and what a dismissal stores. */
    val key: Int,
    val questionId: Int,
    val question: String,
    val failCount: Int,
    val lastWrongAnswer: String,
    val correctAnswer: String
)
