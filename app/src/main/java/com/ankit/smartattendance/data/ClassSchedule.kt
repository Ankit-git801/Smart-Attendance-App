package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var subjectId: Long,
    val dayOfWeek: Int, // 1 for Sunday, 2 for Monday, etc.
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,     // <-- ADDED THIS LINE
    val endMinute: Int    // <-- ADDED THIS LINE
)
