package com.gondroid.mtcquiz.domain.models

import java.time.LocalDateTime

data class Evaluation(
    val id: String,
    val categoryId: String,
    val categoryTitle: String,
    val totalCorrect: Int,
    val totalIncorrect: Int,
    val totalQuestions: Int,
    val state: EvaluationState,
    val date: LocalDateTime = LocalDateTime.now(),
)

enum class EvaluationState {
    APPROVED,
    REJECTED,
}
