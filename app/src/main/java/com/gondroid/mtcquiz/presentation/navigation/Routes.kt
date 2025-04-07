package com.gondroid.mtcquiz.presentation.navigation

import androidx.navigation.NavBackStackEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


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

@Serializable
object TermScreenRoute

@Serializable
object CustomizeScreenRoute

@Serializable
data class SummaryScreenRoute(
    val categoryId: String,
    val evaluationId: String
)

inline fun <reified T : Any> NavBackStackEntry.toRoute(): T {
    val json = arguments?.getString("json") ?: error("Route data missing")
    return Json.decodeFromString<T>(json)
}
