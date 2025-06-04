package com.gondroid.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.gondroid.core.database.entity.EvaluationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {
    @Query("SELECT * FROM evaluations WHERE category_id = :categoryId")
    fun getAllEvaluations(categoryId: String): Flow<List<EvaluationEntity>>

    @Query("SELECT * FROM evaluations WHERE id = :id")
    suspend fun getEvaluationById(id: String): EvaluationEntity?

    @Upsert
    suspend fun upsertEvaluation(note: EvaluationEntity)

    @Query("DELETE FROM evaluations WHERE id = :id")
    suspend fun deleteEvaluationById(id: String)

    @Query("DELETE FROM evaluations")
    suspend fun deleteAllEvaluations()
}