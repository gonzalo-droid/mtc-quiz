package com.gondroid.mtcquiz.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Answer(
    val id: String,
    val title: String,
)
