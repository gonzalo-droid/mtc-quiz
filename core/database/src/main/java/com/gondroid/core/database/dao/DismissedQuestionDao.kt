package com.gondroid.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gondroid.core.database.entity.DismissedQuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DismissedQuestionDao {

    @Query("SELECT question_id FROM dismissed_questions")
    fun getAllDismissedIds(): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun dismiss(entity: DismissedQuestionEntity)

    @Query("DELETE FROM dismissed_questions WHERE question_id = :questionId")
    suspend fun restore(questionId: Int)

    @Query("DELETE FROM dismissed_questions")
    suspend fun restoreAll()
}
