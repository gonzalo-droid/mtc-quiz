package com.gondroid.presentation.screens.evaluation.summary

sealed interface SummaryScreenAction {
    data object FinishExam : SummaryScreenAction
}