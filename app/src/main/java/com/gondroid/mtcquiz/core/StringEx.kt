package com.gondroid.mtcquiz.core

import java.text.Normalizer

fun String.normalizeText(): String {
    return Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}"), "") // Elimina los acentos
        .lowercase() // Convierte a minúsculas
}