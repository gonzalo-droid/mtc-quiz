package com.gondroid.configuration.presentation.customize

sealed interface CustomizeScreenAction {
    data class UpdateValues(
        val numberQuestions: String,
        val timeToFinishEvaluation: String,
        val percentageToApprovedEvaluation: String
    ) : CustomizeScreenAction
}