package com.seoulhankuko.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
        UserSubscription::class
    ],
    version = 1,
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
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "seoul_hankuko_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
