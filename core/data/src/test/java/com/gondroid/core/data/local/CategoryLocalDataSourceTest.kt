package com.gondroid.core.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategoryLocalDataSourceTest {

    @Test
    fun `list has exactly 9 categories, no unimplemented B-I placeholder`() {
        assertThat(categoriesLocalDataSource).hasSize(9)
        assertThat(categoriesLocalDataSource.map { it.category }).doesNotContain("B-I")
    }

    @Test
    fun `class B categories point at their own balotario JSON, not a1`() {
        val byCategory = categoriesLocalDataSource.associateBy { it.category }
        assertThat(byCategory.getValue("B-IIa").pathJson).isEqualTo("b2a_questions.json")
        assertThat(byCategory.getValue("B-IIb").pathJson).isEqualTo("b2b_questions.json")
        assertThat(byCategory.getValue("B-IIc").pathJson).isEqualTo("b2c_questions.json")
    }
}
