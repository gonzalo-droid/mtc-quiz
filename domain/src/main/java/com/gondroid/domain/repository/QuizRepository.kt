package com.gondroid.domain.repository

import com.gondroid.domain.models.Category
import com.gondroid.domain.models.Evaluation
import com.gondroid.domain.models.Question
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