package com.gondroid.mtcquiz.domain.repository

import com.gondroid.mtcquiz.domain.models.Category
import com.gondroid.mtcquiz.domain.models.Evaluation
import com.gondroid.mtcquiz.domain.models.Question
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    val categoriesFlow: Flow<List<Category>>

    suspend fun getCategoryById(categoryId: String): Category?

    suspend fun getQuestionsByCategory(
        categoryId: String,
        isTake: Boolean = false // take while numberQuestion prefer
    ): Flow<List<Question>>

    suspend fun saveEvaluation(evaluation: Evaluation)

    suspend fun getEvaluationById(evaluationId: String): Evaluation?
}