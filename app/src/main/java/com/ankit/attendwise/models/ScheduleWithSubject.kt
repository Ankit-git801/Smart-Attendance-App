package com.ankit.attendwise.models

import com.ankit.attendwise.data.AttendanceRecord
import com.ankit.attendwise.data.ClassSchedule
import com.ankit.attendwise.data.Subject
import java.time.LocalTime

data class ScheduleWithSubject(
    val schedule: ClassSchedule,
    val subject: Subject,
    val attendanceRecord: AttendanceRecord? = null
) {
    val isCurrentClass: Boolean
        get() {
            val now = LocalTime.now()
            val startTime = LocalTime.of(schedule.startHour, schedule.startMinute)
            val endTime = LocalTime.of(schedule.endHour, schedule.endMinute)
            // Inclusive of start time, exclusive of end time
            return (now == startTime || now.isAfter(startTime)) && now.isBefore(endTime)
        }

    val isCompleted: Boolean
        get() {
            val now = LocalTime.now()
            val endTime = LocalTime.of(schedule.endHour, schedule.endMinute)
            return now.isAfter(endTime)
        }
}
