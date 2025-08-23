package com.ankit.smartattendance.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.receivers.AlarmReceiver
import java.util.*

object AlarmScheduler {

    fun scheduleClassAlarms(context: Context, subject: Subject, schedules: List<ClassSchedule>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (schedule in schedules) {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("subject_name", subject.name)
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
                set(Calendar.HOUR_OF_DAY, schedule.endHour)
                set(Calendar.MINUTE, schedule.endMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (this.timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            // Perform the API level check here
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                // For older versions, the permission is not required
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelClassAlarms(context: Context, schedules: List<ClassSchedule>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (schedule in schedules) {
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
}
