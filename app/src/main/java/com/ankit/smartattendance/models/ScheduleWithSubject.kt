package com.ankit.smartattendance.models

import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import java.time.LocalTime

data class ScheduleWithSubject(
    val schedule: ClassSchedule,
    val subject: Subject
) {
    val isCurrentClass: Boolean
        get() {
            val now = LocalTime.now()
            val startTime = LocalTime.of(schedule.startHour, schedule.startMinute)
            val endTime = LocalTime.of(schedule.endHour, schedule.endMinute)
            return now.isAfter(startTime) && now.isBefore(endTime)
        }

    val isCompleted: Boolean
        get() {
            val now = LocalTime.now()
            val endTime = LocalTime.of(schedule.endHour, schedule.endMinute)
            return now.isAfter(endTime)
        }
}
