package com.gondroid.mtcquiz.data.local.quiz

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "question_results")
data class QuestionResultEntity(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,
    val date: Long,
)