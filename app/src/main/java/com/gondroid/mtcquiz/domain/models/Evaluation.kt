package com.gondroid.mtcquiz.domain.models

import java.time.LocalDateTime

data class Evaluation(
    val id: String = "",
    val categoryId: String = "",
    val categoryTitle: String = "",
    val totalCorrect: Int = 0,
    val totalIncorrect: Int= 0,
    val totalQuestions: Int= 0,
    val state: EvaluationState = EvaluationState.APPROVED,
    val date: LocalDateTime = LocalDateTime.now(),
)

enum class EvaluationState {
    APPROVED,
    REJECTED,
}
