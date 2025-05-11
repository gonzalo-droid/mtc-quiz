package com.gondroid.mtcquiz.data.di

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gondroid.mtcquiz.data.local.QuizRepositoryImpl
import com.gondroid.mtcquiz.data.local.evaluation.EvaluationDao
import com.gondroid.mtcquiz.data.local.userPreferences.PreferenceRepositoryImpl
import com.gondroid.mtcquiz.data.remote.AuthRepositoryImpl
import com.gondroid.mtcquiz.domain.repository.AuthRepository
import com.gondroid.mtcquiz.domain.repository.PreferenceRepository
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
object RepositoryModule {

    @Provides
    @Singleton
    fun provideQuizRepository(
        evaluationDao: EvaluationDao,
        preferenceRepository: PreferenceRepository,
        @Named("dispatcherIO")
        dispatcherIO: CoroutineDispatcher,
        @ApplicationContext
        context: Context,
    ): QuizRepository = QuizRepositoryImpl(
        evaluationDao = evaluationDao,
        preferenceRepository = preferenceRepository,
        dispatcherIO = dispatcherIO,
        context = context
    )

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        credentialManager: CredentialManager,
        preferenceRepository: PreferenceRepository
    ): AuthRepository = AuthRepositoryImpl(
        firebaseAuth = firebaseAuth,
        credentialManager = credentialManager,
        preferenceRepository = preferenceRepository
    )

    @Provides
    @Singleton
    fun providePreferenceRepository(
        dataStore: DataStore<Preferences>
    ): PreferenceRepository = PreferenceRepositoryImpl(
        dataStore = dataStore
    )
}