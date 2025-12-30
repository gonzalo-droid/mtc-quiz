package com.gondroid.mtcquiz.data.local

import android.content.Context
import android.content.res.AssetManager
import com.gondroid.core.data.repository.QuizRepositoryImpl
import com.gondroid.core.domain.model.Evaluation
import com.gondroid.mtcquiz.data.local.evaluation.EvaluationDaoFake
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

        val assetManager = mockk<AssetManager>()
        every { assetManager.open("json/a1_questions_test.json") } returns ByteArrayInputStream(json.toByteArray())
        every { mockContext.assets } returns assetManager

        repository = QuizRepositoryImpl(
            evaluationDao = fakeDao,
            dispatcherIO = StandardTestDispatcher(),
            context = mockContext,
            preferenceRepository = mockk()
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
        val questions = repository.getQuestionsByCategory("1").first()
        Truth.assertThat(questions).isNotEmpty()
        Truth.assertThat(questions.first().title).isEqualTo("Está permitido en la vía:")
        val q = questions.first()

    }

}