package com.gondroid.mtcquiz.presentation.screens.questionreview

import com.gondroid.core.domain.model.Question
import com.gondroid.questionreview.presentation.StudyListEntry.BannerEntry
import com.gondroid.questionreview.presentation.StudyListEntry.QuestionEntry
import com.gondroid.questionreview.presentation.questionsBefore
import com.gondroid.questionreview.presentation.withBanners
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StudyListEntryTest {

    private fun questions(count: Int) = List(count) { index ->
        Question(
            id = index + 1,
            title = "Pregunta ${index + 1}",
            topic = "",
            section = "",
            options = emptyList(),
            answer = "a"
        )
    }.withIndex().toList()

    @Test
    fun `a banner follows every tenth question`() {
        val entries = questions(25).withBanners(interval = 10)

        assertThat(entries).hasSize(27)
        assertThat(entries[10]).isEqualTo(BannerEntry(slot = 0))
        assertThat(entries[21]).isEqualTo(BannerEntry(slot = 1))
    }

    @Test
    fun `no banner closes the list`() {
        val entries = questions(20).withBanners(interval = 10)

        assertThat(entries.filterIsInstance<BannerEntry>()).containsExactly(BannerEntry(0))
        assertThat(entries.last()).isInstanceOf(QuestionEntry::class.java)
    }

    @Test
    fun `interval 0 means no banners at all`() {
        val entries = questions(30).withBanners(interval = 0)

        assertThat(entries.filterIsInstance<BannerEntry>()).isEmpty()
        assertThat(entries).hasSize(30)
    }

    @Test
    fun `question keys keep the bank position and never clash with banner keys`() {
        val entries = questions(12).withBanners(interval = 5)

        val keys = entries.map { it.key }
        assertThat(keys).containsNoDuplicates()
        assertThat((entries[6] as QuestionEntry).position).isEqualTo(5)
    }

    @Test
    fun `progress counts questions and skips the banners`() {
        val entries = questions(12).withBanners(interval = 5)
        val before = entries.questionsBefore()

        // Rows: q0..q4, banner, q5..q9, banner, q10, q11
        assertThat(before[5]).isEqualTo(5) // the first banner comes after 5 questions
        assertThat(before[6]).isEqualTo(5) // q5 is still the 6th question
        assertThat(before.last()).isEqualTo(11)
    }
}
