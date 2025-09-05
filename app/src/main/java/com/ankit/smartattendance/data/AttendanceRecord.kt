package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.ColumnInfo

@Entity(
    tableName = "attendance_records",
    // The composite key is now valid because all columns are non-nullable.
    primaryKeys = ["subjectId", "date", "scheduleId", "type"],
)
data class AttendanceRecord(
    val subjectId: Long,
    val date: Long, // Stored as epoch day
    // CORRECTED: scheduleId is now a non-nullable Long.
    // We will use 0L as a sentinel value for extra classes.
    val scheduleId: Long,
    val isPresent: Boolean,
    @ColumnInfo(defaultValue = "CLASS")
    val type: RecordType = RecordType.CLASS,
    val note: String? = null
)

enum class RecordType {
    CLASS,
    MANUAL,
    HOLIDAY
}
