package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "class_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE // If a subject is deleted, its schedules are also deleted
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class ClassSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int, // e.g., Calendar.MONDAY
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val location: String = "" // Optional class location
)
