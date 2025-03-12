package com.gondroid.mtcquiz.data.di

import android.content.Context
import androidx.room.Room
import com.gondroid.mtcquiz.data.MTCDatabase
import com.gondroid.mtcquiz.data.local.quiz.repository.QuizRepositoryImpl
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Named
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class DataModule {
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
    @Singleton
    fun provideQuizRepository(
        @Named("dispatcherIO")
        dispatcherIO: CoroutineDispatcher,
    ): QuizRepository = QuizRepositoryImpl(dispatcherIO)

}