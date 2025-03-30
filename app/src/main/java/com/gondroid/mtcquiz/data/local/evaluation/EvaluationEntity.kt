package com.gondroid.mtcquiz.data.local.evaluation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gondroid.mtcquiz.domain.models.Evaluation
import com.gondroid.mtcquiz.domain.models.EvaluationState
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Entity(tableName = "evaluations")
data class EvaluationEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "category_title")
    val categoryTitle: String,
    @ColumnInfo(name = "total_correct")
    val totalCorrect: Int,
    @ColumnInfo(name = "total_incorrect")
    val totalIncorrect: Int,
    val total: Int,
    val state: String,
    val date: Long,
) {
    companion object {

        fun fromEvaluation(evaluation: Evaluation): EvaluationEntity = EvaluationEntity(
            id = evaluation.id,
            categoryId = evaluation.categoryId,
            categoryTitle = evaluation.categoryTitle,
            totalCorrect = evaluation.totalCorrect,
            totalIncorrect = evaluation.totalIncorrect,
            total = evaluation.totalQuestions,
            state = evaluation.state.name,
            date = evaluation.date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )

    }

    fun toEvaluation(): Evaluation = Evaluation(
        id = id,
        categoryId = categoryId,
        categoryTitle = categoryTitle,
        totalCorrect = totalCorrect,
        totalIncorrect = totalIncorrect,
        totalQuestions = total,
        state = EvaluationState.valueOf(state),
        date = LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault())
    )
}