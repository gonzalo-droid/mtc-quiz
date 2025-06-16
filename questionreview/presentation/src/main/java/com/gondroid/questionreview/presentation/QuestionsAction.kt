package com.gondroid.questionreview.presentation

sealed interface QuestionsAction {
    data object Back : QuestionsAction
}