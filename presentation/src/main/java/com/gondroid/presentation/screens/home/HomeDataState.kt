package com.gondroid.presentation.screens.home

import com.gondroid.domain.models.Category

data class HomeDataState(
    val categories: List<Category> = emptyList(),
    val userName: String = ""
)