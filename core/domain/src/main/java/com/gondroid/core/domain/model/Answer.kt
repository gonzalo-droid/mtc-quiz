package com.gondroid.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Answer(
    val id: String,
    val title: String,
)
