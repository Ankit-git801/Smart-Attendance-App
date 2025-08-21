package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"]), Index(value = ["date"])]
)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val scheduleId: Long?, // Can be null for extra classes
    val date: Long, // Stored as epoch day for easier querying
    val isPresent: Boolean,
    val note: String? = null // Optional note, e.g., "Extra Class"
)
