package com.ankit.smartattendance.models

data class AttendanceStatistics(
    val totalClasses: Int,
    val totalPresent: Int,
    val totalAbsent: Int,
    val overallPercentage: Double,
    val subjectCount: Int
)
