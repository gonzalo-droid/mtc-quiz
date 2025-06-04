package com.gondroid.pdf.presentation

import com.gondroid.domain.models.Category

data class PdfDataState(
    val isLoading: Boolean = false,
    val shouldDownload: Boolean = false,
    val category: Category = Category()
)