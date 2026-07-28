package com.ankit.attendwise.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

import com.google.firebase.firestore.PropertyName

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val subjectId: String = "",
    val scheduleId: String = "",
    val date: Long = 0,
    @get:PropertyName("isPresent")
    @set:PropertyName("isPresent")
    var isPresent: Boolean = false,
    val note: String = "",
    val type: RecordType = RecordType.CLASS,
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class RecordType {
    CLASS,
    CANCELLED,
    HOLIDAY,
    MANUAL
}
