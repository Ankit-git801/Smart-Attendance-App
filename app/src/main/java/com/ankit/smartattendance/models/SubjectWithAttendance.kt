package com.ankit.smartattendance.models

import com.ankit.smartattendance.data.Subject

data class SubjectWithAttendance(
    val subject: Subject,
    val percentage: Double
)
