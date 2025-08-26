package com.ankit.smartattendance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Subject::class, ClassSchedule::class, AttendanceRecord::class],
    version = 2, // <-- THE VERSION IS INCREASED FROM 1 to 2
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_attendance_db"
                )
                    // THIS LINE TELLS ROOM TO DELETE AND RECREATE THE DATABASE
                    // IF THE VERSION NUMBER CHANGES. PERFECT FOR DEVELOPMENT.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
