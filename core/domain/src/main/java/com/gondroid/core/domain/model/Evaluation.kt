package com.gondroid.core.domain.model

import java.time.LocalDateTime
import java.time.ZoneId

data class Evaluation(
    val id: String = "",
    val categoryId: String = "",
    val categoryTitle: String = "",
    val totalCorrect: Int = 0,
    val totalIncorrect: Int = 0,
    val totalQuestions: Int = 0,
    var state: EvaluationState = EvaluationState.APPROVED,
    val date: LocalDateTime = LocalDateTime.now(),
)

enum class EvaluationState {
    APPROVED,
    REJECTED,
}
