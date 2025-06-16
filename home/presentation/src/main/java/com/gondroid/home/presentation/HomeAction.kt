package com.gondroid.home.presentation

sealed interface HomeAction {
    data class OnClickCategory(
        val categoryId: String
    ) : HomeAction

    data object GoToConfiguration : HomeAction
}