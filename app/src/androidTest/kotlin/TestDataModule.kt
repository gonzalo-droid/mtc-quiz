import android.content.Context
import androidx.room.Room
import com.gondroid.mtcquiz.data.MTCDatabase
import com.gondroid.mtcquiz.data.di.DataModule
import com.gondroid.mtcquiz.data.local.evaluation.EvaluationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])
object TestDataModule {
    @Provides
    @Singleton
    fun provideDataBase(
        @ApplicationContext
        context: Context,
    ): MTCDatabase =
        Room
            .inMemoryDatabaseBuilder(
                context.applicationContext,
                MTCDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()

    @Provides
    fun provideEvaluationDao(database: MTCDatabase) : EvaluationDao = database.evaluationDao()
}