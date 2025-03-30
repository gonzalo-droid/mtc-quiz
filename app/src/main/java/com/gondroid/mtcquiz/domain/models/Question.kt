package com.gondroid.mtcquiz.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int = 1,
    val section: String? = "",
    val category: String? = "",
    val topic: String = "",
    val title: String = "",
    val answer: String = "",
    val options: List<String> = arrayListOf(),
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

    fun getOption(answer: String): String {
        return when (answer) {
            "a" -> options[0]
            "b" -> options[1]
            "c" -> options[2]
            else -> options[3]
        }
    }
}

enum class TypeActionQuestion {
    NEXT,
    VERIFY,
    FINISH,
}

@Serializable
data class QuestionResponse(
    val data: List<Question>
)