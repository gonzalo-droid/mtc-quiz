package com.gondroid.mtcquiz.presentation.navigation

import kotlinx.serialization.Serializable


@Serializable
object HomeScreenRoute

@Serializable
data class DetailScreenRoute(
    val categoryId: String
)

@Serializable
object EvaluationScreenRoute


@Serializable
object AllQuestionScreenRoute


@Serializable
object ConfigurationScreenRoute