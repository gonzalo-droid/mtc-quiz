package com.gondroid.mtcquiz.presentation.screens.evaluation

sealed interface EvaluationScreenAction {
    data object StartEvaluation : EvaluationScreenAction

    data object Back : EvaluationScreenAction
}