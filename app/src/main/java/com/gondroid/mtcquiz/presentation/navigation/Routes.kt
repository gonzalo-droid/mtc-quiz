package com.gondroid.mtcquiz.presentation.navigation

import kotlinx.serialization.Serializable


@Serializable
object HomeScreenRoute

@Serializable
data class DetailScreenRoute(
    val categoryId: String
)

@Serializable
data class EvaluationScreenRoute(
    val categoryId: String
)

@Serializable
data class QuestionsScreenRoute(
    val categoryId: String
)

@Serializable
object ConfigurationScreenRoute

@Serializable
object PdfScreenRoute
