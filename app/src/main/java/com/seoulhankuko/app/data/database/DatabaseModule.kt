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
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
        .build()
    }

    @Provides
    @Singleton
    fun provideLoggedAccountDao(database: AppDatabase): LoggedAccountDao {
        return database.loggedAccountDao()
    }

    @Provides
    @Singleton
    fun provideCourseCacheDao(database: AppDatabase): CourseCacheDao {
        return database.courseCacheDao()
    }
}



