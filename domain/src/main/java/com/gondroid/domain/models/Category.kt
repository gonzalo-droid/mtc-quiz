package com.gondroid.domain.models

data class Category(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val classType: String = "",
    val description: String = "",
    val image: String? = "",
    val pdf: String = "",
)
