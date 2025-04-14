package com.gondroid.mtcquiz.presentation.screens.home

import com.gondroid.mtcquiz.domain.models.Category

data class HomeDataState(
    val categories: List<Category> = emptyList(),
    val userName: String = ""
)