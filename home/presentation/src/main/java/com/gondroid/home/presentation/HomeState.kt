package com.gondroid.home.presentation

import com.gondroid.core.domain.model.Category

data class HomeState(
    val categories: List<Category> = emptyList(),
    val userName: String = "",
    val streak: Int = 0,
    val isPremium: Boolean = false,
    // true por defecto, igual que el default local de Remote Config
    val isPremiumOfferEnabled: Boolean = true
)
