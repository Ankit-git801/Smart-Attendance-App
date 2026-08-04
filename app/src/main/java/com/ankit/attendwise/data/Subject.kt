package com.ankit.attendwise.data

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Keep
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey
    val id: String = "",
    val name: String = "",
    val color: String = "#4CAF50",
    val targetAttendance: Int = 75,
    val lastUpdated: Long = System.currentTimeMillis()
)
