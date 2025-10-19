package com.seoulhankuko.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seoulhankuko.app.data.database.daos.*
import com.seoulhankuko.app.data.database.entities.*

@Database(
    entities = [
        Course::class,
        UnitEntity::class,
        Lesson::class,
        Challenge::class,
        ChallengeOption::class,
        UserProgress::class,
        ChallengeProgress::class,
        UserSubscription::class,
        LoggedAccountEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun unitDao(): UnitDao
    abstract fun lessonDao(): LessonDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun challengeOptionDao(): ChallengeOptionDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun challengeProgressDao(): ChallengeProgressDao
    abstract fun loggedAccountDao(): LoggedAccountDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Migration from version 1 to 2: Add logged_accounts table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `logged_accounts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `email` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `photoUrl` TEXT,
                        `accessToken` TEXT,
                        `refreshToken` TEXT,
                        `lastLogin` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seoul_hankuko_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
