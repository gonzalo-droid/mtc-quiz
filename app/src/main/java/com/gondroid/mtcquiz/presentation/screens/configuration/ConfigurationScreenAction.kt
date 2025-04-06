package com.gondroid.mtcquiz.presentation.screens.configuration

sealed interface ConfigurationScreenAction {
    data object GoToTerm : ConfigurationScreenAction

    data object GoToSetting : ConfigurationScreenAction

    data object Logout : ConfigurationScreenAction

    data object GoToRating : ConfigurationScreenAction

    data object GoToAbout : ConfigurationScreenAction
}