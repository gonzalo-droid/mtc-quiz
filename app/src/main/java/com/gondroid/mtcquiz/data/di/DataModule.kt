package com.gondroid.mtcquiz.data.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.room.Room
import com.gondroid.mtcquiz.data.MTCDatabase
import com.gondroid.mtcquiz.data.local.QuizRepositoryImpl
import com.gondroid.mtcquiz.data.local.evaluation.EvaluationDao
import com.gondroid.mtcquiz.data.remote.AuthRepositoryImpl
import com.gondroid.mtcquiz.domain.repository.AuthRepository
import com.gondroid.mtcquiz.domain.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
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
    fun provideEvaluationDao(database: MTCDatabase) = database.evaluationDao()

    @Provides
    @Singleton
    fun provideQuizRepository(
        evaluationDao: EvaluationDao,
        @Named("dispatcherIO")
        dispatcherIO: CoroutineDispatcher,
        @ApplicationContext
        context: Context,
    ): QuizRepository = QuizRepositoryImpl(
        evaluationDao = evaluationDao,
        dispatcherIO = dispatcherIO,
        context = context
    )

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        credentialManager: CredentialManager,
        @ApplicationContext
        context: Context,
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth = firebaseAuth, credentialManager =credentialManager,context = context)

}