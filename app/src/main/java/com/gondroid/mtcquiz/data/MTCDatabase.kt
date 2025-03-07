package com.gondroid.mtcquizz.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gondroid.mtcquizz.data.local.quiz.QuestionResultEntity


@Database(
    entities = [
        QuestionResultEntity::class,
    ],
    version = 1,
)
abstract class MTCDatabase : RoomDatabase() {

}