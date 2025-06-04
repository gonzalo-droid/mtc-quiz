package com.gondroid.evaluation.presentation

sealed interface EvaluationEvent {
    data class EvaluationCreated(val evaluationId: String) : EvaluationEvent
}