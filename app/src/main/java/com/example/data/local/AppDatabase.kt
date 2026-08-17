package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PetEntity::class,
        PlayerEntity::class,
        InventoryEntity::class,
        DailyMissionEntity::class,
        AchievementEntity::class,
        GameStatsEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pet ADD COLUMN hatchedTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pet ADD COLUMN youthTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pet ADD COLUMN adultTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pet ADD COLUMN seniorTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bichinho_virtual.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
