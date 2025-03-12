package com.gondroid.mtcquiz.domain.repository

import com.gondroid.mtcquiz.domain.models.Category
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    val categoriesFlow: Flow<List<Category>>

    suspend fun getCategoryById(categoryId: String): Category?
}