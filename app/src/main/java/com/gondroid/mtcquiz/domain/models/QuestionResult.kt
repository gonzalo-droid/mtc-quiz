package com.gondroid.mtcquiz.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class QuestionResult(
    val id: String,
    val questionId: Int,
    val question: String,
    val option: String? = "",
    val isCorrect: Boolean = false
)