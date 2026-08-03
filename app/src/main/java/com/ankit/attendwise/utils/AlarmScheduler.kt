package com.ankit.attendwise.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.attendwise.data.ClassSchedule
import com.ankit.attendwise.data.Subject
import com.ankit.attendwise.receivers.AlarmReceiver
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleClassAlarm(context: Context, subject: Subject, schedule: ClassSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("subject_id", subject.id)
            putExtra("schedule_id", schedule.id)
            putExtra("subject_name", subject.name ?: "Unknown")
        }

        val requestCode = schedule.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // SAFETY GUARD: Prevent crash if data is malformed
        if (schedule.dayOfWeek !in 1..7 || schedule.endHour !in 0..23 || schedule.endMinute !in 0..59) {
            Log.w(TAG, "Skipping invalid schedule for ${subject.name}: Day=${schedule.dayOfWeek}, Time=${schedule.endHour}:${schedule.endMinute}")
            return
        }

        // MODERN FIX: Using java.time for robust scheduling
        // Convert Calendar day (1=Sun, 2=Mon) to java.time.DayOfWeek (1=Mon, 7=Sun)
        val targetDayOfWeek = if (schedule.dayOfWeek == 1) DayOfWeek.SUNDAY else DayOfWeek.of(schedule.dayOfWeek - 1)
        val targetTime = LocalTime.of(schedule.endHour, schedule.endMinute)
        
        val now = LocalDateTime.now()
        var alarmDateTime = now.with(TemporalAdjusters.nextOrSame(targetDayOfWeek))
            .with(targetTime)
            .withSecond(0)
            .withNano(0)

        // If the calculated time for today has already passed, schedule for next week
        if (alarmDateTime.isBefore(now.plusSeconds(10))) {
            alarmDateTime = alarmDateTime.with(TemporalAdjusters.next(targetDayOfWeek))
        }

        val triggerTimeMs = alarmDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        Log.d(TAG, "Scheduling alarm for ${subject.name ?: "Unknown"} at: ${alarmDateTime.format(formatter)}")

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(triggerTimeMs, null)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Log.d(TAG, "Alarm successfully set.")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    fun cancelClassAlarm(context: Context, schedule: ClassSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Canceled alarm for schedule ID: ${schedule.id}")
        }
    }
}
