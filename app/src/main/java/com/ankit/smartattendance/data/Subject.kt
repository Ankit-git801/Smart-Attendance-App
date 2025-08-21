package com.ankit.smartattendance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String, // Stored as a hex string, e.g., "#FF5733"
    val targetAttendance: Int = 75 // Default attendance target
)
