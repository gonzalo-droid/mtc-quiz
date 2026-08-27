package com.gondroid.mtcquiz.data.local

import android.content.Context
import android.content.res.AssetManager
import com.gondroid.core.data.repository.QuizRepositoryImpl
import com.gondroid.core.domain.model.Evaluation
import com.gondroid.mtcquiz.data.local.evaluation.EvaluationDaoFake
import com.gondroid.mtcquiz.presentation.screens.PreferenceRepositoryFake
import com.gondroid.mtcquiz.util.MainDispatcherRule
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream

class QuizRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: QuizRepositoryImpl
    private lateinit var fakeDao: EvaluationDaoFake
    private lateinit var assetManager: AssetManager
    private lateinit var preferenceRepositoryFake: PreferenceRepositoryFake
    private val mockContext = mockk<Context>()

    @Before
    fun setUp() {
        fakeDao = EvaluationDaoFake()

        val json = """
            {
                "data": [
                     {
                          "id": 1,
                          "section": "Materias generales",
                          "category": "AI",
                          "topic": "Reglamento de Tránsito y Manual de Dispositivos de Control de Tránsito",
                          "title": "Está permitido en la vía:",
                          "answer": "c",
                          "options": [
                            "a) Recoger o dejar pasajeros o carga en cualquier lugar",
                            "b) Dejar animales sueltos o situarlos de forma tal que obstaculicen solo un poco el tránsito",
                            "c) Recoger o dejar pasajeros en lugares autorizados.",
                            "d) Ejercer el comercio ambulatorio o estacionario"
                          ]
                    }
                ]
            }
        """.trimIndent()

        assetManager = mockk<AssetManager>()
        every { assetManager.open("json/a1_questions_test.json") } returns ByteArrayInputStream(json.toByteArray())
        every { mockContext.assets } returns assetManager

        preferenceRepositoryFake = PreferenceRepositoryFake()
        repository = QuizRepositoryImpl(
            evaluationDao = fakeDao,
            dispatcherIO = StandardTestDispatcher(),
            context = mockContext,
            preferenceRepository = preferenceRepositoryFake
        )
    }

    @Test
    fun `getEvaluationById returns correct Evaluation`() = runTest {
        val result = repository.getEvaluationById("3")
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result?.id).isEqualTo("3")
        Truth.assertThat(result?.categoryTitle).isEqualTo("Category 3")
    }

    @Test
    fun `saveEvaluation adds new evaluation to dao`() = runTest {
        val newEval = Evaluation(
            id = "6",
            categoryId = "1",
            categoryTitle = "Category 6",
            totalCorrect = 15,
            totalIncorrect = 0
        )

        repository.saveEvaluation(newEval)

        val result = repository.getEvaluationById("6")
        Truth.assertThat(result).isNotNull()
        Truth.assertThat(result?.id).isEqualTo("6")
        Truth.assertThat(result?.categoryTitle).isEqualTo("Category 6")
    }

    @Test
    fun `getQuestionsByCategory returns question from json`() = runTest {
        val questions = repository.getQuestionsByCategory("1", "a1_questions_test.json").first()
        Truth.assertThat(questions).isNotEmpty()
        Truth.assertThat(questions.first().title).isEqualTo("Está permitido en la vía:")
        val q = questions.first()
    }

    @Test
    fun `getQuestionsByCategory returns a different selection across repeated evaluations`() = runTest {
        val questionsJson = (1..20).joinToString(prefix = "[", postfix = "]", separator = ",") {
            """{"id": $it, "title": "Q$it"}"""
        }
        val json = """{"data": $questionsJson}"""
        // `answers` (not `returns`) so each of the repeated calls below gets a fresh, unread
        // stream — reusing one instance would exhaust it after the first read and EOF the rest.
        every { assetManager.open("json/many_questions_test.json") } answers { ByteArrayInputStream(json.toByteArray()) }
        preferenceRepositoryFake.setNumberQuestions("5")

        val selections = (1..20).map {
            repository.getQuestionsByCategory("1", "many_questions_test.json", isTake = true).first().map { q -> q.title }
        }

        // Regression guard for the "same questions every evaluation" bug: with 20 questions and
        // a random 5-question sample, seeing the identical selection on every one of 20 runs
        // would only happen if the shuffle is missing (or broken), not by chance.
        Truth.assertThat(selections.toSet().size).isGreaterThan(1)
    }
}
