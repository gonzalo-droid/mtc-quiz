package com.gondroid.questionreview.presentation

sealed interface QuestionsScreenAction {
    data object Back : QuestionsScreenAction
}