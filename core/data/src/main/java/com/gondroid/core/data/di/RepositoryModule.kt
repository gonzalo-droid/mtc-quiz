package com.gondroid.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gondroid.core.data.repository.PreferenceRepositoryImpl
import com.gondroid.core.data.repository.QuizRepositoryImpl
import com.gondroid.core.database.dao.EvaluationDao
import com.gondroid.core.domain.repository.PreferenceRepository
import com.gondroid.core.domain.repository.QuizRepository
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
    fun providePreferenceRepository(
        dataStore: DataStore<Preferences>
    ): PreferenceRepository = PreferenceRepositoryImpl(
        dataStore = dataStore
    )
}