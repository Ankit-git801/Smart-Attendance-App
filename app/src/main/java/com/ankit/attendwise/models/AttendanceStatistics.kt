package com.ankit.attendwise.models

import androidx.annotation.Keep

@Keep
data class AttendanceStatistics(
    val totalClasses: Int,
    val totalPresent: Int,
    val totalAbsent: Int,
    val overallPercentage: Double,
    val subjectCount: Int
)
