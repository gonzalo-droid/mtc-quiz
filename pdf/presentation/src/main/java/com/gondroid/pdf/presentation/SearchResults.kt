package com.gondroid.pdf.presentation

import android.graphics.RectF

data class SearchResults(
    val page: Int,
    val results: List<RectF>
)