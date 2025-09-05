package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.ColumnInfo

@Entity(
    tableName = "attendance_records",
    primaryKeys = ["subjectId", "date", "scheduleId", "type"]
)
data class AttendanceRecord(
    val subjectId: Long,
    val date: Long,
    val scheduleId: Long,
    val isPresent: Boolean,
    @ColumnInfo(defaultValue = "CLASS")
    val type: RecordType = RecordType.CLASS,
    val note: String? = null
)

// THIS IS THE FIX: The CANCELLED type must be defined here.
enum class RecordType {
    CLASS,
    MANUAL,
    HOLIDAY,
    CANCELLED
}
