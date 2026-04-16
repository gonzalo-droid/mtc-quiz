package com.gondroid.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dismissed_questions")
data class DismissedQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: Int,
)
