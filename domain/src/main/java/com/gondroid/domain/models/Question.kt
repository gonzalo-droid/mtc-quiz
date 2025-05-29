package com.gondroid.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Question(
    val id: Int = 1,
    val section: String? = "",
    val category: String? = "",
    val topic: String = "",
    val title: String = "",
    val answer: String = "",
    val options: List<String> = listOf(),
    val image: String? = null,
) {
    fun isCorrectAnswer(index: Int): Boolean {
        val indexAnswer = when (answer) {
            "a" -> 0
            "b" -> 1
            "c" -> 2
            else -> 3
        }
        return index == indexAnswer
    }

    fun getOption(letter: String): String {
        val index = when (letter.lowercase()) {
            "a" -> 0
            "b" -> 1
            "c" -> 2
            "d" -> 3
            else -> -1
        }
        return options.getOrNull(index) ?: "Opción no disponible"
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