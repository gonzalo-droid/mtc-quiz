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
) {
    fun validationAnswer(index: Int): Boolean {
        val indexAnswer = when (answer) {
            "a" -> 0
            "b" -> 1
            "c" -> 2
            else -> 3
        }
        return index == indexAnswer
    }
}


@Serializable
data class QuestionResponse(
    val data: List<Question>
)