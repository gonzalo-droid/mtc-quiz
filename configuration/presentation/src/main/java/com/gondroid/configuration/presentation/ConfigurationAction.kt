package com.gondroid.configuration.presentation

sealed interface ConfigurationAction {
    data object GoToTerm : ConfigurationAction
    data object GoToSCustomize : ConfigurationAction
    data object Logout : ConfigurationAction
    data object GoToRating : ConfigurationAction
    data object GoToAbout : ConfigurationAction
    data object GoToTarifas : ConfigurationAction
    data object GoToStats : ConfigurationAction
    data object GoToHistory : ConfigurationAction
    data object GoToPremium : ConfigurationAction
    data class ToggleDarkMode(val enabled: Boolean) : ConfigurationAction
    data class SetThemeMode(val mode: String) : ConfigurationAction
}