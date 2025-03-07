package com.gondroid.mtcquiz.data.di

import android.content.Context
import androidx.room.Room
import com.gondroid.mtcquiz.data.MTCDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
            // Esto elimina y recrea la base de datos en cambios de esquema
            // solo para entorno de prueba
            .fallbackToDestructiveMigration()
            .build()

}