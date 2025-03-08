package com.gondroid.mtcquiz.domain.models

data class Category(
    val id: String,
    val title: String,
    val category: String,
    val type: String,
    val description: String,
    val image: String? = null,
    val pdf: String? = null,
)
