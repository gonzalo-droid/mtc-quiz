package com.gondroid.core.database.di

import android.content.Context
import androidx.room.Room
import com.gondroid.core.database.MTCDatabase
import com.gondroid.core.database.dao.EvaluationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideDataBase(
        @ApplicationContext
        context: Context,
    ): MTCDatabase =
        Room
            .databaseBuilder(
                context.applicationContext,
                MTCDatabase::class.java,
                "mtc_database",
            )
            .build()

    @Provides
    fun provideEvaluationDao(database: MTCDatabase): EvaluationDao = database.evaluationDao()
}