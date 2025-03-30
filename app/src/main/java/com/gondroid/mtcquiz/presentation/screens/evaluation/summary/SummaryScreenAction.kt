package com.gondroid.mtcquiz.presentation.screens.evaluation.summary

sealed interface SummaryScreenAction {
    data object FinishExam : SummaryScreenAction
    data object GoToQuestions : SummaryScreenAction
}