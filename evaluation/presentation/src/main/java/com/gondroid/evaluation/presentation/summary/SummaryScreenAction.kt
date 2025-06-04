package com.gondroid.evaluation.presentation.summary

sealed interface SummaryScreenAction {
    data object FinishExam : SummaryScreenAction
}