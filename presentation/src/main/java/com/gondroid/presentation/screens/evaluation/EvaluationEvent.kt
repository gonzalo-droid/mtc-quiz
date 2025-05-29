package com.gondroid.presentation.screens.evaluation

sealed interface EvaluationEvent {
    data class EvaluationCreated(val evaluationId: String) : EvaluationEvent
}