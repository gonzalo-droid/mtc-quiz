package com.gondroid.evaluation.presentation.review

import com.gondroid.core.domain.model.Evaluation
import com.gondroid.core.domain.model.QuestionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequentErrorsTest {

    private fun failed(questionId: Int, question: String) = QuestionResult(
        id = "$questionId-$question",
        questionId = questionId,
        question = question,
        option = "a",
        isCorrect = false,
        correctAnswer = "b"
    )

    private fun evaluation(categoryId: String, vararg results: QuestionResult) =
        Evaluation(categoryId = categoryId, questionResults = results.toList())

    @Test
    fun `errorKey matches the reference FNV-1a values that iOS pins too`() {
        // Same values in MTCEvaluationFeatureTests/FrequentErrorsTests.swift.
        assertEquals(237896178, errorKey("a1", 5, "Pregunta"))
        assertEquals(
            -1597544078,
            errorKey("a3b", 63, "¿A partir de qué edad los menores de edad deben ocupar su propio asiento?")
        )
    }

    @Test
    fun `the same id in two banks is two different errors`() {
        val evaluations = List(3) {
            evaluation("a1", failed(5, "Pregunta de A-I"))
        } + evaluation("b2c", failed(5, "Pregunta de B-IIC"))

        val errors = frequentErrors(evaluations, emptySet())

        assertEquals(1, errors.size)
        assertEquals("Pregunta de A-I", errors.single().question)
        assertEquals(3, errors.single().failCount)
    }

    @Test
    fun `a repeated id inside a3b does not merge its two questions`() {
        // a3b's PDF restarts numbering for its second table: id 63 is two different questions.
        val first = failed(63, "¿A partir de qué año de antigüedad debe realizarse la inspección técnica?")
        val second = failed(63, "¿A partir de qué edad los menores de edad deben ocupar su propio asiento?")
        val evaluations = listOf(
            evaluation("a3b", first, second),
            evaluation("a3b", first),
            evaluation("a3b", second)
        )

        assertTrue(frequentErrors(evaluations, emptySet()).isEmpty())
    }

    @Test
    fun `dismissing one error leaves the same id in another bank listed`() {
        val evaluations = List(3) {
            evaluation("a1", failed(5, "Pregunta de A-I"))
        } + List(4) {
            evaluation("b2c", failed(5, "Pregunta de B-IIC"))
        }
        val dismissed = setOf(errorKey("b2c", 5, "Pregunta de B-IIC"))

        val errors = frequentErrors(evaluations, dismissed)

        assertEquals(listOf("Pregunta de A-I"), errors.map { it.question })
    }

    @Test
    fun `a dismissal stored as a bare question id no longer hides anything`() {
        val evaluations = List(3) { evaluation("a1", failed(5, "Pregunta de A-I")) }

        assertEquals(1, frequentErrors(evaluations, setOf(5)).size)
    }

    @Test
    fun `errors are sorted by fail count`() {
        val evaluations = List(3) { evaluation("a1", failed(1, "Uno")) } +
            List(5) { evaluation("a1", failed(2, "Dos")) }

        assertEquals(listOf("Dos", "Uno"), frequentErrors(evaluations, emptySet()).map { it.question })
    }
}
