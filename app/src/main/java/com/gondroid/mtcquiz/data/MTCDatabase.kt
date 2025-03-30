package com.gondroid.mtcquiz.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gondroid.mtcquiz.data.local.evaluation.EvaluationDao
import com.gondroid.mtcquiz.data.local.evaluation.EvaluationEntity

@Database(
    entities = [
        EvaluationEntity::class,
    ],
    version = 1,
)
abstract class MTCDatabase : RoomDatabase() {
    abstract fun evaluationDao(): EvaluationDao
}