package com.gondroid.presentation.screens.questions

sealed interface QuestionsScreenAction {
    data object Back : QuestionsScreenAction
}