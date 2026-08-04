package com.gondroid.mtcquiz.data.local

import com.gondroid.core.domain.model.QuestionResponse
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

/**
 * Validates structural invariants of every question bank shipped under
 * `app/src/main/assets/json/`. These files are produced by extraction
 * scripts from source PDFs (see
 * `.claude/skills/mtc-question-extractor/SKILL.md`) and are otherwise
 * untyped at build time, so this test is the safety net against extraction
 * regressions (truncation, mis-parsed answers, malformed image filenames,
 * etc).
 */
class QuestionAssetsSchemaTest {

    private val jsonDir = File("src/main/assets/json")
    private val validLetters = setOf("a", "b", "c", "d")

    // Mirrors the Json config QuizRepositoryImpl actually uses to decode
    // these same asset files at runtime (core/data/.../QuizRepositoryImpl.kt).
    // Some balotario JSON assets (a3b, a3c) carry an extra "part" bookkeeping
    // field not present in the Question schema; ignoring unknown keys here
    // keeps this test's parsing behavior faithful to production instead of
    // failing on a field production itself tolerates.
    private val json = Json { ignoreUnknownKeys = true }

    private fun questionFiles(): List<File> =
        jsonDir.listFiles { f -> f.name.endsWith("_questions.json") && f.name != "a1_questions_test.json" }
            ?.sortedBy { it.name }
            ?: error("assets/json directory not found at ${jsonDir.absolutePath}")

    private fun questionsOf(file: File): QuestionResponse =
        json.decodeFromString(file.readText())

    @Test
    fun `every question file parses and has 4 options with a valid answer`() {
        val errors = mutableListOf<String>()
        for (file in questionFiles()) {
            val response = questionsOf(file)
            for (q in response.data) {
                if (q.options.size != 4) {
                    errors += "${file.name}#${q.id}: expected 4 options, got ${q.options.size}"
                }
                if (q.answer.lowercase() !in validLetters) {
                    errors += "${file.name}#${q.id}: answer '${q.answer}' not in a-d"
                }
                if (q.title.isBlank()) {
                    errors += "${file.name}#${q.id}: blank title"
                }
                if (q.options.any { it.isBlank() }) {
                    errors += "${file.name}#${q.id}: blank option"
                }
                val answerIndex = "abcd".indexOf(q.answer.lowercase().firstOrNull() ?: ' ')
                if (q.options.getOrNull(answerIndex).isNullOrBlank()) {
                    errors += "${file.name}#${q.id}: answer '${q.answer}' points at a blank option"
                }
            }
        }
        assertThat(errors).isEmpty()
    }

    /**
     * Most balotario source PDFs contain a single numbered question table,
     * so their extracted `id`s are unique and contiguous from 1..N. Three
     * source PDFs are verified, documented structural outliers and get a
     * narrower (but still regression-sensitive) check instead of the strict
     * one:
     *
     * - `CLASE_A_IIIB.pdf` (-> a3b_questions.json) and `CLASE_A_IIIC.pdf`
     *   (-> a3c_questions.json) each contain *two* independently numbered
     *   tables whose own "Nº" column restarts at 1 for the second table.
     *   The JSON preserves those literal (duplicate) PDF ids by design --
     *   see `.claude/skills/mtc-question-extractor/SKILL.md`, which
     *   documents that the extraction scripts had to specifically work
     *   around this. This is legitimate, pre-existing legacy data, not a
     *   bug: a3b's ids run 1..200 then 1..71 (271 records total), a3c's run
     *   1..200 then 1..139 (339 records total).
     *
     * - `CLASE_A_IIB.pdf` (-> a2b_questions.json) also has a second internal
     *   table ("materias específicas"), but unlike a3b/a3c its ids continue
     *   the running document count (offset by 200) rather than restarting
     *   at 1. However the source PDF's own local "Nº" column in that second
     *   table skips from 66 straight to 68 (confirmed directly against the
     *   PDF text with `pdftotext -layout`: question 66 "Es el servicio de
     *   transporte..." is immediately followed by question 68 "Escoja el
     *   elemento que no pertenece al sistema de transmisión...", with no 67
     *   anywhere in between). So the global id sequence is missing 267 --
     *   a genuine one-question gap in the government's own document, not an
     *   extraction defect.
     *
     * A future truncation or stray-duplicate regression in any of these
     * three files would still fail the narrower checks below (segment
     * lengths / the exact expected-minus-gap set would no longer match).
     */
    @Test
    fun `question ids within each file are unique and correctly sequenced`() {
        val restartFiles = setOf("a3b_questions.json", "a3c_questions.json")
        val knownGaps = mapOf("a2b_questions.json" to setOf(267))

        val errors = mutableListOf<String>()
        for (file in questionFiles()) {
            val ids = questionsOf(file).data.map { it.id }
            errors += when {
                file.name in restartFiles -> validateRestartSequence(file.name, ids)
                file.name in knownGaps -> validateGappedSequence(file.name, ids, knownGaps.getValue(file.name))
                else -> validateStrictSequence(file.name, ids)
            }
        }
        assertThat(errors).isEmpty()
    }

    private fun validateStrictSequence(fileName: String, ids: List<Int>): List<String> {
        val errors = mutableListOf<String>()
        if (ids.toSet().size != ids.size) {
            errors += "$fileName: duplicate ids found"
        }
        val expected = (1..ids.size).toList()
        if (ids.sorted() != expected) {
            errors += "$fileName: ids not contiguous 1..${ids.size}, got range ${ids.min()}..${ids.max()}"
        }
        return errors
    }

    /**
     * Splits [ids] at every point the sequence drops back down (a
     * "restart") and requires each resulting segment to be exactly
     * `1..segment.size`, in order. Exactly one restart is expected (the
     * second internal table); zero restarts, more than one, or a
     * mis-sized/out-of-order segment all indicate real corruption.
     */
    private fun validateRestartSequence(fileName: String, ids: List<Int>): List<String> {
        val errors = mutableListOf<String>()
        val segments = mutableListOf<List<Int>>()
        var segmentStart = 0
        for (i in 1 until ids.size) {
            if (ids[i] < ids[i - 1]) {
                segments += ids.subList(segmentStart, i)
                segmentStart = i
            }
        }
        segments += ids.subList(segmentStart, ids.size)

        if (segments.size != 2) {
            errors += "$fileName: expected exactly one id-sequence restart (known second-table numbering), found ${segments.size - 1}"
        }
        segments.forEachIndexed { index, segment ->
            val expected = (1..segment.size).toList()
            if (segment != expected) {
                errors += "$fileName: segment ${index + 1} is not contiguous 1..${segment.size}, got " +
                    "${segment.firstOrNull()}..${segment.lastOrNull()}"
            }
        }
        return errors
    }

    /**
     * Requires unique ids matching `1..max` with exactly the [knownMissing]
     * ids absent (a documented, verified gap in the source PDF's own
     * numbering) -- nothing else missing, and no duplicates.
     */
    private fun validateGappedSequence(fileName: String, ids: List<Int>, knownMissing: Set<Int>): List<String> {
        val errors = mutableListOf<String>()
        if (ids.toSet().size != ids.size) {
            errors += "$fileName: duplicate ids found"
        }
        val max = ids.maxOrNull() ?: 0
        val expected = (1..max).toList() - knownMissing
        if (ids.sorted() != expected) {
            errors += "$fileName: ids don't match expected sequence 1..$max minus known gap(s) $knownMissing"
        }
        return errors
    }

    @Test
    fun `imagens entries follow the q-id_letter_examId naming convention`() {
        val pattern = Regex("""^q\d+_[a-z]_[a-z0-9]+$""")
        val errors = mutableListOf<String>()
        for (file in questionFiles()) {
            val response = questionsOf(file)
            for (q in response.data) {
                for (name in q.imagens) {
                    if (!pattern.matches(name)) {
                        errors += "${file.name}#${q.id}: imagens entry '$name' doesn't match naming convention"
                    }
                }
            }
        }
        assertThat(errors).isEmpty()
    }
}
