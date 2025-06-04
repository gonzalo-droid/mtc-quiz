package com.gondroid.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gondroid.core.database.dao.EvaluationDao
import com.gondroid.core.database.entity.EvaluationEntity

@Database(
    entities = [
        EvaluationEntity::class,
    ],
    version = 1,
)
abstract class MTCDatabase : RoomDatabase() {
    abstract fun evaluationDao(): EvaluationDao
}