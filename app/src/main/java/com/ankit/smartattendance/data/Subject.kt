package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val color: String = "",
    val targetAttendance: Int = 75,
    val lastUpdated: Long = System.currentTimeMillis()
)
