package com.gondroid.home.presentation

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryColorsTest {

    private val fallback = CategoryColorScheme(container = Color.Gray, content = Color.Black)

    @Test
    fun `known category codes map to their documented colors`() {
        val expected = mapOf(
            "A-I" to CategoryColorScheme(Color(0xFF274C93), Color.White),
            "A-IIa" to CategoryColorScheme(Color(0xFF3461B3), Color.White),
            "A-IIb" to CategoryColorScheme(Color(0xFF3F76D6), Color.White),
            "A-IIIa" to CategoryColorScheme(Color(0xFF5C8CE0), Color.White),
            "A-IIIb" to CategoryColorScheme(Color(0xFF7BA3E8), Color(0xFF12233F)),
            "A-IIIc" to CategoryColorScheme(Color(0xFF9EBCEF), Color(0xFF12233F)),
            "B-IIa" to CategoryColorScheme(Color(0xFFB5651D), Color.White),
            "B-IIb" to CategoryColorScheme(Color(0xFFD07A2B), Color.White),
            "B-IIc" to CategoryColorScheme(Color(0xFFE89A4D), Color(0xFF12233F))
        )
        expected.forEach { (code, colorScheme) ->
            assertThat(categoryColors(code, fallback)).isEqualTo(colorScheme)
        }
    }

    @Test
    fun `unknown category code falls back to the provided default`() {
        assertThat(categoryColors("Z-999", fallback)).isEqualTo(fallback)
    }
}
