package com.gondroid.presentation.screens.pdf

import com.gondroid.domain.models.Category

data class PdfDataState(
    val isLoading: Boolean = false,
    val shouldDownload: Boolean = false,
    val category: Category = Category()
)