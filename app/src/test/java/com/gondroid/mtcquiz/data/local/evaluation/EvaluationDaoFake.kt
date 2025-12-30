package com.gondroid.mtcquiz.data.local.evaluation

import com.gondroid.core.database.dao.EvaluationDao
import com.gondroid.core.database.entity.EvaluationEntity
import com.gondroid.core.database.mapper.toDomain
import com.gondroid.core.database.mapper.toEntity
import com.gondroid.core.domain.model.Evaluation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class EvaluationDaoFake : EvaluationDao {

    var evaluations = (1..5).map {
        Evaluation(
            id = it.toString(),
            categoryId = "1",
            categoryTitle = "Category $it",
            totalCorrect = 10,
            totalIncorrect = 5,
        )
    }.toMutableList()

    override fun getAllEvaluations(categoryId: String): Flow<List<EvaluationEntity>> {
        return flow { emit(evaluations.map { it.toEntity() }) }
    }

    override suspend fun getEvaluationById(id: String): EvaluationEntity? {
        return evaluations.find { it.id == id }?.toEntity()
    }

    override suspend fun upsertEvaluation(evaluation: EvaluationEntity) {
        val element = evaluations.find { it.id == evaluation.id }
        if (element != null) {
            evaluations.remove(element)
        }
        evaluations.add(evaluation.toDomain())
    }

    override suspend fun deleteEvaluationById(id: String) {
        val element = evaluations.find { it.id == id }
        if (element != null) {
            evaluations.remove(element)
        }
    }

    override suspend fun deleteAllEvaluations() {
        evaluations.clear()
    }

}