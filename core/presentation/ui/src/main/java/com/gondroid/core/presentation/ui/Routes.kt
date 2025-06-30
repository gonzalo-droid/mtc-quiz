package com.gondroid.core.presentation.ui

import kotlinx.serialization.Serializable

@Serializable
object LoginScreenRoute

@Serializable
object HomeScreenRoute

@Serializable
object ConfigurationScreenRoute

@Serializable
object TermScreenRoute

@Serializable
object CustomizeScreenRoute

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
data class PdfScreenRoute(
    val categoryId: String
)

@Serializable
data class SummaryScreenRoute(
    val categoryId: String,
    val evaluationId: String
)
