package com.gondroid.mtcquiz.data.local.quiz.repository

import android.content.Context
import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.domain.models.Question
import com.gondroid.mtcquiz.domain.models.QuestionResponse
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.IOException

class QuizRepositoryImpl(
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

    override suspend fun getQuestionsByCategory(categoryId: String): Flow<List<Question>> = channelFlow {
        launch(dispatcherIO) { // Se ejecuta en IO
            val questions = try {
                val jsonString = context.assets.open("json/a1_questions.json")
                    .bufferedReader().use { it.readText() }

                val questionResponse = Json.decodeFromString<QuestionResponse>(jsonString)
                questionResponse.data // Devuelve la lista de preguntas

            } catch (e: IOException) {
                emptyList() // En caso de error, devuelve una lista vacía
            }

            send(questions) // Usa send() para enviar los datos al flujo
            close() // Cierra el canal después de enviar los datos
        }
    }
}