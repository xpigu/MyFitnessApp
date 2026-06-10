package com.example.myfitnessapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myfitnessapp.data.dao.AchievementBadgeDao
import com.example.myfitnessapp.data.dao.CustomFoodDao
import com.example.myfitnessapp.data.dao.DailyCheckinDao
import com.example.myfitnessapp.data.dao.DietRecordDao
import com.example.myfitnessapp.data.dao.FavoriteMealComboDao
import com.example.myfitnessapp.data.dao.UserProfileDao
import com.example.myfitnessapp.data.dao.WorkoutRecordDao
import com.example.myfitnessapp.data.entity.AchievementBadge
import com.example.myfitnessapp.data.entity.CustomFood
import com.example.myfitnessapp.data.entity.DailyCheckin
import com.example.myfitnessapp.data.entity.DietRecord
import com.example.myfitnessapp.data.entity.FavoriteMealCombo
import com.example.myfitnessapp.data.entity.UserProfile
import com.example.myfitnessapp.data.entity.WorkoutRecord

@Database(
    entities = [
        WorkoutRecord::class,
        DietRecord::class,
        UserProfile::class,
        AchievementBadge::class,
        DailyCheckin::class,
        CustomFood::class,
        FavoriteMealCombo::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun workoutRecordDao(): WorkoutRecordDao
    abstract fun dietRecordDao(): DietRecordDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun achievementBadgeDao(): AchievementBadgeDao
    abstract fun dailyCheckinDao(): DailyCheckinDao
    abstract fun customFoodDao(): CustomFoodDao
    abstract fun favoriteMealComboDao(): FavoriteMealComboDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
