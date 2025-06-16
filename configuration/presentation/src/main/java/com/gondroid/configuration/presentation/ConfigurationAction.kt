package com.gondroid.configuration.presentation

sealed interface ConfigurationAction {
    data object GoToTerm : ConfigurationAction

    data object GoToSCustomize : ConfigurationAction

    data object Logout : ConfigurationAction

    data object GoToRating : ConfigurationAction

    data object GoToAbout : ConfigurationAction
}