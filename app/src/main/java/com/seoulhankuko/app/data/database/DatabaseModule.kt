package com.seoulhankuko.app.data.database

import android.content.Context
import androidx.room.Room
import com.seoulhankuko.app.data.database.daos.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Room database dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "seoul_hankuko_database"
        )
        .addMigrations(AppDatabase.MIGRATION_1_2)
        .build()
    }

    @Provides
    @Singleton
    fun provideCourseDao(database: AppDatabase): CourseDao {
        return database.courseDao()
    }

    @Provides
    @Singleton
    fun provideUnitDao(database: AppDatabase): UnitDao {
        return database.unitDao()
    }

    @Provides
    @Singleton
    fun provideLessonDao(database: AppDatabase): LessonDao {
        return database.lessonDao()
    }

    @Provides
    @Singleton
    fun provideChallengeDao(database: AppDatabase): ChallengeDao {
        return database.challengeDao()
    }

    @Provides
    @Singleton
    fun provideChallengeOptionDao(database: AppDatabase): ChallengeOptionDao {
        return database.challengeOptionDao()
    }

    @Provides
    @Singleton
    fun provideUserProgressDao(database: AppDatabase): UserProgressDao {
        return database.userProgressDao()
    }

    @Provides
    @Singleton
    fun provideChallengeProgressDao(database: AppDatabase): ChallengeProgressDao {
        return database.challengeProgressDao()
    }

    @Provides
    @Singleton
    fun provideLoggedAccountDao(database: AppDatabase): LoggedAccountDao {
        return database.loggedAccountDao()
    }
}



