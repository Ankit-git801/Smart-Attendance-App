package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// We add a new MANUAL type to the enum
enum class RecordType {
    CLASS, HOLIDAY, MANUAL
}

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long?,
    val scheduleId: Long?,
    val date: Long,
    val isPresent: Boolean,
    val note: String? = null,
    val type: RecordType = RecordType.CLASS
)
