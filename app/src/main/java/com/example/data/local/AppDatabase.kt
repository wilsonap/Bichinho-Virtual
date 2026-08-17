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
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pet ADD COLUMN disease TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE pet ADD COLUMN lowHygieneExposure INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pet ADD COLUMN exhaustionCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pet ADD COLUMN indigestionStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pet ADD COLUMN lastDoctorCheckupTimestamp INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bichinho_virtual.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
