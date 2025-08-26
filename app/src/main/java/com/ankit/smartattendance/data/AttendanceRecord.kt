package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE // This is the key change
        )
    ]
)
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

enum class RecordType {
    CLASS, HOLIDAY, MANUAL
}
