package com.gondroid.detail.presentation

import com.gondroid.core.domain.model.Category

data class DetailState(
    val category: Category = Category(),
    val isLoading: Boolean = false,
    val error: String? = null
)