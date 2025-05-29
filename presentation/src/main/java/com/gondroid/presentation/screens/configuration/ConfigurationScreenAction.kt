package com.gondroid.presentation.screens.configuration

sealed interface ConfigurationScreenAction {
    data object GoToTerm : ConfigurationScreenAction

    data object GoToSCustomize : ConfigurationScreenAction

    data object Logout : ConfigurationScreenAction

    data object GoToRating : ConfigurationScreenAction

    data object GoToAbout : ConfigurationScreenAction
}