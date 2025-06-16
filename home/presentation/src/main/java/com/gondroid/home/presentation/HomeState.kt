package com.gondroid.home.presentation

import com.gondroid.core.domain.model.Category


data class HomeState(
    val categories: List<Category> = emptyList(),
    val userName: String = ""
)