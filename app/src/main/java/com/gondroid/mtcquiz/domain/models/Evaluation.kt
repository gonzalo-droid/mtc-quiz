package com.gondroid.mtcquiz.domain.models

import com.gondroid.mtcquiz.data.local.evaluation.EvaluationEntity
import java.time.LocalDateTime
import java.time.ZoneId

data class Evaluation(
    val id: String = "",
    val categoryId: String = "",
    val categoryTitle: String = "",
    val totalCorrect: Int = 0,
    val totalIncorrect: Int = 0,
    val totalQuestions: Int = 0,
    val state: EvaluationState = EvaluationState.APPROVED,
    val date: LocalDateTime = LocalDateTime.now(),
) {
    fun toEntity(): EvaluationEntity = EvaluationEntity(
        id = id,
        categoryId = categoryId,
        categoryTitle = categoryTitle,
        totalCorrect = totalCorrect,
        totalIncorrect = totalIncorrect,
        total = totalQuestions,
        state = state.name,
        date = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
}

enum class EvaluationState {
    APPROVED,
    REJECTED,
}
