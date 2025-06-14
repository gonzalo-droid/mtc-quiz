package com.gondroid.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class QuestionResult(
    val id: String,
    val questionId: Int,
    val question: String,
    val option: String? = "",
    val isCorrect: Boolean = false
)