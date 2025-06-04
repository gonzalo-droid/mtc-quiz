package com.gondroid.home.presentation

import com.gondroid.domain.models.Category

data class HomeDataState(
    val categories: List<Category> = emptyList(),
    val userName: String = ""
)