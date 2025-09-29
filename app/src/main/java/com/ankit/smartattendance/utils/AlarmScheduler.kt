package com.ankit.smartattendance.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.receivers.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

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

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, schedule.dayOfWeek)
            set(Calendar.HOUR_OF_DAY, schedule.startHour)
            set(Calendar.MINUTE, schedule.startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has already passed for today, schedule it for the same day next week.
            if (this.timeInMillis <= System.currentTimeMillis()) {
                this.add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        // DEFINITIVE FIX: Using setAlarmClock for maximum reliability.
        // This method is designed for user-facing alarms and is less likely to be suppressed by the system.
        // The first parameter is an AlarmClockInfo object which tells the system this is an important alarm.
        // The second null parameter is an optional intent to show when the user clicks the clock icon in the status bar.
        val alarmClockInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, null)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
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
        }
    }
}
