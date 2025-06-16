package com.gondroid.core.data.repository

import android.content.Context
import com.gondroid.core.data.local.categoriesLocalDataSource
import com.gondroid.core.database.dao.EvaluationDao
import com.gondroid.core.database.mapper.toDomain
import com.gondroid.core.database.mapper.toEntity
import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.Evaluation
import com.gondroid.core.domain.model.EvaluationState
import com.gondroid.core.domain.model.Question
import com.gondroid.core.domain.model.QuestionResponse
import com.gondroid.core.domain.repository.PreferenceRepository
import com.gondroid.core.domain.repository.QuizRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlinx.serialization.json.Json

class QuizRepositoryImpl(
    private val evaluationDao: EvaluationDao,
    private val preferenceRepository: PreferenceRepository,
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO,
    private val context: Context,
) : QuizRepository {
    override val categoriesFlow: Flow<List<Category>>
        get() = flow {
            emit(categoriesLocalDataSource)
        }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return categoriesLocalDataSource.find { it.id == categoryId }
    }

    override suspend fun getQuestionsByCategory(
        categoryId: String,
        isTake: Boolean
    ): Flow<List<Question>> =
        channelFlow {
            launch(dispatcherIO) { // Se ejecuta en IO
                val questions = try {
                    val jsonString = context.assets.open("json/a1_questions_test.json")
                        .bufferedReader().use { it.readText() }

                    val questionResponse =
                        Json.Default.decodeFromString<QuestionResponse>(jsonString)

                    val numberQuestion =
                        preferenceRepository.numberQuestionsFlow.first().toIntOrNull()

                    if (isTake && numberQuestion != null) {
                        questionResponse.data.take(numberQuestion)
                    } else {
                        questionResponse.data
                    }
                } catch (e: IOException) {
                    emptyList() // En caso de error, devuelve una lista vacía
                }

                send(questions) // Usa send() para enviar los datos al flujo
                close() // Cierra el canal después de enviar los datos
            }
        }

    override suspend fun saveEvaluation(evaluation: Evaluation) = withContext(dispatcherIO) {

        val percentage =
            (evaluation.totalCorrect / evaluation.totalQuestions.toFloat()).times(100).toInt()

        val preferencePercentage =
            preferenceRepository.percentageToApprovedEvaluationFlow.first().toIntOrNull()

        preferencePercentage?.let {
            evaluation.state = if (percentage >= preferencePercentage) {
                EvaluationState.APPROVED
            } else {
                EvaluationState.REJECTED
            }
        }
        evaluationDao.upsertEvaluation(evaluation.toEntity())
    }

    override suspend fun getEvaluationById(evaluationId: String): Evaluation? =
        withContext(dispatcherIO) {
            evaluationDao.getEvaluationById(evaluationId)?.toDomain()
        }
}