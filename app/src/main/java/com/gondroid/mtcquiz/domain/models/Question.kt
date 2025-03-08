package com.gondroid.mtcquiz.domain.models

data class Question(
    val id: String,
    val title: String,
    val topic: String,
    val answers: List<Answer>,
    val correctAnswer: String,
    val image: String? = null,
)
