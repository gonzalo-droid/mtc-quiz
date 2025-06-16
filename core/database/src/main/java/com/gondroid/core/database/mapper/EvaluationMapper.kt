package com.gondroid.core.database.mapper

import com.gondroid.core.database.entity.EvaluationEntity
import com.gondroid.core.domain.model.Evaluation
import com.gondroid.core.domain.model.EvaluationState
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


fun EvaluationEntity.toDomain(): Evaluation = Evaluation(
    id = id,
    categoryId = categoryId,
    categoryTitle = categoryTitle,
    totalCorrect = totalCorrect,
    totalIncorrect = totalIncorrect,
    totalQuestions = total,
    state = EvaluationState.valueOf(state),
    date = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault())
)


fun Evaluation.toEntity(): EvaluationEntity = EvaluationEntity(
    id = id,
    categoryId = categoryId,
    categoryTitle = categoryTitle,
    totalCorrect = totalCorrect,
    totalIncorrect = totalIncorrect,
    total = totalQuestions,
    state = state.name,
    date = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
)
