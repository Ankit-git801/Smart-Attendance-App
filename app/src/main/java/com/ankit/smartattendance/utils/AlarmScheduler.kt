package com.ankit.smartattendance.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.receivers.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleClassAlarm(context: Context, subject: Subject, schedule: ClassSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("subject_id", subject.id)
            putExtra("schedule_id", schedule.id)
        }

        val requestCode = schedule.id.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // DEFINITIVE FIX: Using the class END TIME (endHour, endMinute) to set the alarm.
        val alarmTime = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, schedule.dayOfWeek)
            // Use the end hour and minute for the alarm trigger.
            set(Calendar.HOUR_OF_DAY, schedule.endHour)
            set(Calendar.MINUTE, schedule.endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the calculated end time for today has already passed, schedule it for the same day next week.
        if (alarmTime.timeInMillis <= System.currentTimeMillis()) {
            alarmTime.add(Calendar.DATE, 7)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Log.d(TAG, "Scheduling alarm for ${subject.name} to trigger at: ${dateFormat.format(Date(alarmTime.timeInMillis))}")

        try {
            val clockInfo = AlarmManager.AlarmClockInfo(alarmTime.timeInMillis, null)
            alarmManager.setAlarmClock(clockInfo, pendingIntent)
            Log.d(TAG, "Alarm successfully scheduled using setAlarmClock.")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    fun cancelClassAlarm(context: Context, schedule: ClassSchedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Canceled alarm for schedule ID: ${schedule.id}")
        }
    }
}
