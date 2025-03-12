package com.gondroid.mtcquiz.presentation.screens.detail

import com.gondroid.mtcquiz.domain.models.Category

data class DetailDataState(
    val date: String = "",
    val category: Category = Category()
)