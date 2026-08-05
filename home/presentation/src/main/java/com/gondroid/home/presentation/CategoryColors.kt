package com.gondroid.home.presentation

import androidx.compose.ui.graphics.Color

data class CategoryColorScheme(
    val container: Color,
    val content: Color,
)

private val categoryColorMap = mapOf(
    "A-I" to CategoryColorScheme(Color(0xFF274C93), Color.White),
    "A-IIa" to CategoryColorScheme(Color(0xFF3461B3), Color.White),
    "A-IIb" to CategoryColorScheme(Color(0xFF3F76D6), Color.White),
    "A-IIIa" to CategoryColorScheme(Color(0xFF5C8CE0), Color.White),
    "A-IIIb" to CategoryColorScheme(Color(0xFF7BA3E8), Color(0xFF12233F)),
    "A-IIIc" to CategoryColorScheme(Color(0xFF9EBCEF), Color(0xFF12233F)),
    "B-IIa" to CategoryColorScheme(Color(0xFFB5651D), Color.White),
    "B-IIb" to CategoryColorScheme(Color(0xFFD07A2B), Color.White),
    "B-IIc" to CategoryColorScheme(Color(0xFFE89A4D), Color(0xFF12233F)),
)

fun categoryColors(category: String, fallback: CategoryColorScheme): CategoryColorScheme =
    categoryColorMap[category] ?: fallback
