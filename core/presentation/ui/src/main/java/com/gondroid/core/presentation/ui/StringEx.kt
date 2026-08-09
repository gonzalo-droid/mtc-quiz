package com.gondroid.core.presentation.ui

import android.annotation.SuppressLint
import java.text.Normalizer

fun String.normalizeText(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "") // Elimina los acentos
        .lowercase() // Convierte a minúsculas
}

private val OPTION_LETTER_PREFIX = Regex("^[a-dA-D]\\)\\s*")

/**
 * Strips the leading letter prefix (e.g. "a) ") that raw [com.gondroid.core.domain.model.Question.options]
 * strings embed, returning just the option's display text.
 */
fun String.stripOptionLetterPrefix(): String = replaceFirst(OPTION_LETTER_PREFIX, "")

@SuppressLint("DefaultLocale")
fun Int.toFormattedTime(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format("%02d:%02d", minutes, seconds)
}
