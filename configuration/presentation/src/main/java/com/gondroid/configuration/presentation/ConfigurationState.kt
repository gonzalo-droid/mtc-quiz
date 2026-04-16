package com.gondroid.configuration.presentation

data class ConfigurationState(
    val date: String = "",
    val isDarkMode: Boolean = false,
    val isPremium: Boolean = false,
    val themeMode: String = "system",
)