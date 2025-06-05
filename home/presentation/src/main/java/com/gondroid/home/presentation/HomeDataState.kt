package com.gondroid.home.presentation


data class HomeDataState(
    val categories: List<Category> = emptyList(),
    val userName: String = ""
)