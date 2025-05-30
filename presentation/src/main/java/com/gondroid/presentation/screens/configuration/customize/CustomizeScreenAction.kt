package com.gondroid.presentation.screens.configuration.customize

sealed interface CustomizeScreenAction {
    data class UpdateValues(
        val numberQuestions: String,
        val timeToFinishEvaluation: String,
        val percentageToApprovedEvaluation: String
    ) : CustomizeScreenAction
}