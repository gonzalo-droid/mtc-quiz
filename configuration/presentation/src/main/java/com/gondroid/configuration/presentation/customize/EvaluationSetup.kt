package com.gondroid.configuration.presentation.customize

import kotlin.math.ceil

/**
 * Los tres ajustes de una evaluación y lo que se deduce de ellos.
 *
 * La pantalla pedía tres números sueltos y no decía qué examen componían: con 40 preguntas
 * y 80 % hay que acertar 32, y eso el usuario lo calculaba de cabeza. Estas propiedades son
 * las que la pantalla muestra mientras se mueven los controles.
 */
data class EvaluationSetup(
    val minutes: Int,
    val questions: Int,
    val passPercentage: Int
) {
    /** Aciertos necesarios para aprobar. Se redondea hacia arriba: con 10 al 75 % son 8, no 7. */
    val correctToPass: Int
        get() = if (questions <= 0) 0 else ceil(questions * passPercentage / 100.0).toInt()

    /** Fallos que caben sin suspender. */
    val allowedMistakes: Int
        get() = (questions - correctToPass).coerceAtLeast(0)

    /** Segundos disponibles por pregunta, truncados: es el tiempo con el que se puede contar. */
    val secondsPerQuestion: Int
        get() = if (questions <= 0) 0 else minutes * 60 / questions

    val pace: Pace
        get() = when {
            questions <= 0 -> Pace.COMFORTABLE
            secondsPerQuestion >= 60 -> Pace.COMFORTABLE
            secondsPerQuestion >= 30 -> Pace.TIGHT
            else -> Pace.AGAINST_THE_CLOCK
        }
}

/**
 * Qué tan exigente queda la combinación. Los cortes son un minuto y medio minuto por
 * pregunta: el examen oficial reparte 40 preguntas en 40 minutos, o sea un minuto justo.
 */
enum class Pace {
    COMFORTABLE,
    TIGHT,
    AGAINST_THE_CLOCK
}
