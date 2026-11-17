package com.gondroid.questionreview.presentation

import com.gondroid.core.domain.model.Question

/** One row of the study list: a question, or a banner between questions. */
sealed interface StudyListEntry {
    /** Stable LazyColumn key: question keys and banner keys never collide. */
    val key: Any

    /**
     * [position] is the question's index in the full bank. It is the key because question.id
     * repeats in a3b/a3c (their PDF restarts numbering for its second table).
     */
    data class QuestionEntry(val position: Int, val question: Question) : StudyListEntry {
        override val key: Any get() = position
    }

    /** [slot] numbers the banners from 0, so each one keeps its own ad while scrolling. */
    data class BannerEntry(val slot: Int) : StudyListEntry {
        override val key: Any get() = "banner-$slot"
    }
}

/**
 * Puts a banner after every [interval] questions, never after the last one. An [interval] of 0
 * or less means no banners (premium, ads turned off remotely, or a search in progress).
 */
fun List<IndexedValue<Question>>.withBanners(interval: Int): List<StudyListEntry> {
    val entries = ArrayList<StudyListEntry>(size + if (interval > 0) size / interval else 0)
    var slot = 0
    forEachIndexed { shown, (position, question) ->
        entries += StudyListEntry.QuestionEntry(position, question)
        val isLast = shown == lastIndex
        if (interval > 0 && !isLast && (shown + 1) % interval == 0) {
            entries += StudyListEntry.BannerEntry(slot++)
        }
    }
    return entries
}

/**
 * How many questions come before each list row, so the "x/N" progress keeps counting questions
 * and not the banners in between.
 */
fun List<StudyListEntry>.questionsBefore(): IntArray {
    val before = IntArray(size)
    var count = 0
    forEachIndexed { index, entry ->
        before[index] = count
        if (entry is StudyListEntry.QuestionEntry) count++
    }
    return before
}
