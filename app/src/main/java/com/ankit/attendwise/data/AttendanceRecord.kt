package com.ankit.attendwise.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

import com.google.firebase.firestore.PropertyName

@Keep
@Entity(
    tableName = "attendance_records",
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["date"])
    ]
)
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

@Keep
enum class RecordType {
    CLASS,
    CANCELLED,
    HOLIDAY,
    MANUAL
}
