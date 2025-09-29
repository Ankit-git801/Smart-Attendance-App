package com.ankit.smartattendance.models

import androidx.room.Embedded
import com.ankit.smartattendance.data.AttendanceRecord

/**
 * A data class that combines an [AttendanceRecord] with additional subject details.
 * This is used for displaying attendance history with the name and color of the subject.
 */
data class AttendanceRecordWithSubject(
    @Embedded
    val attendanceRecord: AttendanceRecord,
    val subjectName: String?,
    // DEFINITIVE FIX: The color is a String (e.g., "#FF5733"), not an Int.
    val subjectColor: String?
)
