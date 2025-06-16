package com.gondroid.pdf.presentation

import com.gondroid.core.domain.model.Category


data class PdfState(
    val isLoading: Boolean = false,
    val shouldDownload: Boolean = false,
    val category: Category = Category()
)