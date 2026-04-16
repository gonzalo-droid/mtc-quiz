package com.gondroid.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gondroid.core.database.dao.EvaluationDao
import com.gondroid.core.database.entity.EvaluationEntity

@Database(
    entities = [
        EvaluationEntity::class,
    ],
    version = 2,
)
abstract class MTCDatabase : RoomDatabase() {
    abstract fun evaluationDao(): EvaluationDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE evaluations ADD COLUMN question_results TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }
    }
}