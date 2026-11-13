package com.gondroid.evaluation.presentation.review

import com.gondroid.core.domain.model.Evaluation

/** A question has to be failed this many times before "Revisar errores" lists it. */
internal const val FREQUENT_ERROR_THRESHOLD = 3

/**
 * Identifies one real question across every evaluation: bank + PDF id + text.
 *
 * The id alone is not enough. Every bank numbers from 1, so "question 5" exists in all nine, and
 * a3b/a3c repeat ids inside the same bank because their PDF restarts the numbering for its second
 * table. Grouping or dismissing by id merged unrelated questions.
 *
 * The key is stored in `dismissed_questions.question_id`, so it has to be an Int and stable across
 * launches: FNV-1a (32 bit) over the UTF-8 bytes. iOS computes the same value
 * (`FrequentErrors.swift`), and both test suites pin the same reference hashes.
 */
internal fun errorKey(categoryId: String, questionId: Int, question: String): Int {
    var hash = 0x811c9dc5.toInt()
    for (byte in "$categoryId|$questionId|$question".toByteArray(Charsets.UTF_8)) {
        hash = hash xor (byte.toInt() and 0xff)
        hash *= 0x01000193
    }
    return hash
}

/**
 * The questions failed at least [FREQUENT_ERROR_THRESHOLD] times and not dismissed, most failed
 * first. `dismissedKeys` holds [errorKey]s; rows written before keys existed hold bare question
 * ids, which match no key, so those old dismissals simply stop applying.
 */
internal fun frequentErrors(
    evaluations: List<Evaluation>,
    dismissedKeys: Collection<Int>
): List<FrequentError> =
    evaluations
        .flatMap { evaluation ->
            evaluation.questionResults
                .filter { !it.isCorrect }
                .map { errorKey(evaluation.categoryId, it.questionId, it.question) to it }
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .filter { (key, results) ->
            results.size >= FREQUENT_ERROR_THRESHOLD && key !in dismissedKeys
        }
        .map { (key, results) ->
            val latest = results.last()
            FrequentError(
                key = key,
                questionId = latest.questionId,
                question = latest.question,
                failCount = results.size,
                lastWrongAnswer = latest.option ?: "",
                correctAnswer = latest.correctAnswer
            )
        }
        .sortedByDescending { it.failCount }
