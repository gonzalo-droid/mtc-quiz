package com.gondroid.mtcquiz.presentation.screens

import com.gondroid.core.data.local.CLASS_A
import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.Evaluation
import com.gondroid.core.domain.model.Question
import com.gondroid.core.domain.repository.QuizRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class QuizRepositoryFake : QuizRepository {

    private val fakeCategories = listOf(
        Category(
            id = "1",
            title = "CLASE A - CATEGORIA I",
            category = "A-I",
            classType = CLASS_A,
            description = "Es el más común y te permite manejar carros como sedanes, coupé , hatchback, convertibles, station wagon, SUV, Areneros, Pickup y furgones. Es necesaria para obtener las demás licencias de Clase A.",
            image = "a",
            pdf = "CLASE_A_I.pdf"
        ),
        Category(
            id = "2",
            title = "CLASE A - CATEGORIA II-A",
            category = "A-IIa",
            classType = CLASS_A,
            description = "Los mismos que A-1 y también carros oficiales de transporte de pasajeros como Taxis, Buses, Ambulancias y Transporte Interprovincial. Primero debes obtener la Licencia A-I",
            image = "a",
            pdf = "CLASE_A_I.pdf"
        ),
    )

    private val fakeQuestions = mapOf(
        "1" to listOf(
            Question(
                id = 1,
                section = "Materias generales",
                category = "AI",
                topic = "Reglamento de Tránsito y Manual de Dispositivos de Control de Tránsito",
                title = "Respecto de los dispositivos de control o regulación del tránsito:",
                answer = "a",
                options = listOf("París", "Madrid", "Roma", "Lisboa")
            ),
            Question(
                id = 2,
                section = "A",
                category = "General",
                topic = "Cultura",
                title = "¿Qué idioma se habla en Brasil?",
                answer = "b",
                options = listOf("Español", "Portugués", "Francés", "Inglés")
            )
        ),
        "2" to listOf(
            Question(
                id = 3,
                section = "B",
                category = "Tecnología",
                topic = "Informática",
                title = "¿Qué es HTML?",
                answer = "c",
                options = listOf("Un lenguaje de backend", "Un compilador", "Un lenguaje de marcado", "Una base de datos")
            )
        )
    )

    private val fakeEvaluations = mutableListOf<Evaluation>()

    override val categoriesFlow: Flow<List<Category>>
        get() = flow { emit(fakeCategories) }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return fakeCategories.find { it.id == categoryId }
    }

    override suspend fun getQuestionsByCategory(
        categoryId: String,
        isTake: Boolean
    ): Flow<List<Question>> {
        val questions = fakeQuestions[categoryId] ?: emptyList()
        return flow { emit(questions) }
    }

    override suspend fun saveEvaluation(evaluation: Evaluation) {
        fakeEvaluations.removeIf { it.id == evaluation.id }
        fakeEvaluations.add(evaluation)
    }

    override suspend fun getEvaluationById(evaluationId: String): Evaluation? {
        return fakeEvaluations.find { it.id == evaluationId }
    }
}