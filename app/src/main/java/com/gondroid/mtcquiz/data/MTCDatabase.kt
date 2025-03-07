package com.gondroid.mtcquiz.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gondroid.mtcquiz.data.local.quiz.QuestionResultEntity


@Database(
    entities = [
        QuestionResultEntity::class,
    ],
    version = 1,
)
abstract class MTCDatabase : RoomDatabase() {

}