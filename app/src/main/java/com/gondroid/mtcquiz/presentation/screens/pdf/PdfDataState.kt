package com.gondroid.mtcquiz.presentation.screens.pdf

import com.gondroid.mtcquiz.domain.models.Category

data class PdfDataState(
    val isLoading: Boolean = false,
    val shouldDownload: Boolean = false,
    val category: Category = Category()
)