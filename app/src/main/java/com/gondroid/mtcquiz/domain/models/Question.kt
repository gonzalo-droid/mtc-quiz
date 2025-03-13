package com.gondroid.mtcquiz.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int,
    val section: String? = "",
    val category: String? = "",
    val topic: String,
    val title: String,
    val answer: String,
    val options: List<String>,
    val image: String? = null,
)


@Serializable
data class QuestionResponse(
    val data: List<Question>
)