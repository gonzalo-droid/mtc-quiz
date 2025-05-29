package com.gondroid.presentation.screens.home

sealed interface HomeScreenAction {
    data class OnClickCategory(
        val categoryId: String
    ) : HomeScreenAction

    data object GoToConfiguration : HomeScreenAction
}