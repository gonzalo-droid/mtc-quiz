package com.gondroid.home.presentation

sealed interface HomeScreenAction {
    data class OnClickCategory(
        val categoryId: String
    ) : HomeScreenAction

    data object GoToConfiguration : HomeScreenAction
}