package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0, // This is the crucial missing piece.
    val subjectId: Long,
    val scheduleId: Long,
    val date: Long,
    val isPresent: Boolean,
    val note: String,
    val type: RecordType
)

enum class RecordType {
    CLASS,
    CANCELLED,
    HOLIDAY,
    MANUAL
}
