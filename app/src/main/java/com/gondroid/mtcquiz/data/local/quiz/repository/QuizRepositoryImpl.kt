package com.gondroid.mtcquiz.data.local.quiz.repository

import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class QuizRepositoryImpl(
    private val dispatcherIO: CoroutineDispatcher = Dispatchers.IO,
) : QuizRepository {
    override val categoriesFlow: Flow<List<Category>>
        get() = flow {
            emit(categoriesLocalDataSource)
        }

    override suspend fun getCategoryById(categoryId: String): Category? {
        return categoriesLocalDataSource.find { it.id == categoryId }
    }
}