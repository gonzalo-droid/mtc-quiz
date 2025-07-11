package com.gondroid.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
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
)