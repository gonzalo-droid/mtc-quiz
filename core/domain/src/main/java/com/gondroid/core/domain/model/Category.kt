package com.gondroid.core.domain.model

data class Category(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val classType: String = "",
    val description: String = "",
    val image: Int? = null,
    val pdf: String = "",
    val pathJson: String = "",
)