package com.gondroid.configuration.presentation.customize

sealed interface CustomizeAction {
    data class UpdateValues(
        val numberQuestions: String,
        val timeToFinishEvaluation: String,
        val percentageToApprovedEvaluation: String
    ) : CustomizeAction
}