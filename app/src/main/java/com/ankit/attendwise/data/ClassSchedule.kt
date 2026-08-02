package com.ankit.attendwise.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "class_schedules",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["dayOfWeek"])
    ]
)
data class ClassSchedule(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    var subjectId: String = "",
    val dayOfWeek: Int = 0,
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 0,
    val endMinute: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
