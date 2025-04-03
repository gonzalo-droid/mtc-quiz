package com.gondroid.mtcquiz.core

import android.annotation.SuppressLint
import java.text.Normalizer

fun String.normalizeText(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "") // Elimina los acentos
        .lowercase() // Convierte a minúsculas
}


@SuppressLint("DefaultLocale")
fun Int.toFormattedTime(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    return String.format("%02d:%02d", hours, minutes)
}
