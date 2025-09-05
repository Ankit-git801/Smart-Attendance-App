package com.ankit.smartattendance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Subject::class, ClassSchedule::class, AttendanceRecord::class],
    // STEP 1: The version number is increased from 1 to 2.
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // STEP 2: We define the migration path from version 1 to 2.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // This is the SQL command to add the 'note' column to our existing table.
                database.execSQL("ALTER TABLE attendance_records ADD COLUMN note TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_attendance_db"
                )
                    // STEP 3: We add our migration to the database builder.
                    // This tells Room to run our migration script when it detects
                    // an upgrade from version 1 to 2.
                    .addMigrations(MIGRATION_1_2)
                    // Optional: This will delete and recreate the database if a migration path
                    // is not found. It's useful during development but should be removed
                    // for a production release to avoid data loss.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
