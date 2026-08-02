package com.gondroid.evaluation.presentation.summary

sealed interface SummaryAction {
    data object FinishExam : SummaryAction
}