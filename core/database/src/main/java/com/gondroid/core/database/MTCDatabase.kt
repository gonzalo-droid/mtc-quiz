package com.gondroid.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.gondroid.core.database.dao.DismissedQuestionDao
import com.gondroid.core.database.dao.EvaluationDao
import com.gondroid.core.database.entity.DismissedQuestionEntity
import com.gondroid.core.database.entity.EvaluationEntity

@Database(
    entities = [
        EvaluationEntity::class,
        DismissedQuestionEntity::class,
    ],
    version = 3,
)
abstract class MTCDatabase : RoomDatabase() {
    abstract fun evaluationDao(): EvaluationDao
    abstract fun dismissedQuestionDao(): DismissedQuestionDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE evaluations ADD COLUMN question_results TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS dismissed_questions (question_id INTEGER NOT NULL PRIMARY KEY)"
                )
            }
        }
    }
}
