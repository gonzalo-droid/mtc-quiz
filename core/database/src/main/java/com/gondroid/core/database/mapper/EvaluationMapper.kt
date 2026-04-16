package com.gondroid.core.database.mapper

import com.gondroid.core.database.entity.EvaluationEntity
import com.gondroid.core.domain.model.Evaluation
import com.gondroid.core.domain.model.EvaluationState
import com.gondroid.core.domain.model.QuestionResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Serializable
data class QuestionResultJson(
    val id: String,
    val questionId: Int,
    val question: String,
    val option: String? = "",
    val isCorrect: Boolean = false,
)

private val json = Json { ignoreUnknownKeys = true }

fun EvaluationEntity.toDomain(): Evaluation {
    val results = try {
        json.decodeFromString<List<QuestionResultJson>>(questionResults).map {
            QuestionResult(
                id = it.id,
                questionId = it.questionId,
                question = it.question,
                option = it.option,
                isCorrect = it.isCorrect,
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
    return Evaluation(
        id = id,
        categoryId = categoryId,
        categoryTitle = categoryTitle,
        totalCorrect = totalCorrect,
        totalIncorrect = totalIncorrect,
        totalQuestions = total,
        state = EvaluationState.valueOf(state),
        date = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()),
        questionResults = results,
    )
}

fun Evaluation.toEntity(): EvaluationEntity {
    val resultsJson = try {
        json.encodeToString(
            questionResults.map {
                QuestionResultJson(
                    id = it.id,
                    questionId = it.questionId,
                    question = it.question,
                    option = it.option,
                    isCorrect = it.isCorrect,
                )
            }
        )
    } catch (e: Exception) {
        "[]"
    }
    return EvaluationEntity(
        id = id,
        categoryId = categoryId,
        categoryTitle = categoryTitle,
        totalCorrect = totalCorrect,
        totalIncorrect = totalIncorrect,
        total = totalQuestions,
        state = state.name,
        date = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        questionResults = resultsJson,
    )
}
