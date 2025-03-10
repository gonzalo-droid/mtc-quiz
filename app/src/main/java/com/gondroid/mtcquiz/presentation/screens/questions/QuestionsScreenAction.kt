package com.gondroid.mtcquiz.presentation.screens.questions

sealed interface QuestionsScreenAction {
    data object Back : QuestionsScreenAction
}