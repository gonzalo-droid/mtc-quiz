package com.gondroid.core.domain.repository

import com.gondroid.core.domain.model.Category
import com.gondroid.core.domain.model.Evaluation
import com.gondroid.core.domain.model.Question
import kotlinx.coroutines.flow.Flow

interface QuizRepository {
    val categoriesFlow: Flow<List<Category>>

    suspend fun getCategoryById(categoryId: String): Category?

    suspend fun getQuestionsByCategory(
        categoryId: String,
        pathJson: String,
        // take while numberQuestion prefer
        isTake: Boolean = false
    ): Flow<List<Question>>

    suspend fun saveEvaluation(evaluation: Evaluation)

    suspend fun getEvaluationById(evaluationId: String): Evaluation?

    fun getAllEvaluations(): Flow<List<Evaluation>>
}
