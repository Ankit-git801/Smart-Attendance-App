package com.ankit.smartattendance.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subjectName = intent.getStringExtra("subject_name")
        val subjectId = intent.getLongExtra("subject_id", -1L)
        val scheduleId = intent.getLongExtra("schedule_id", -1L)

        // Use goAsync() to handle background work correctly
        val pendingResult: PendingResult = goAsync()
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            try {
                if (subjectName != null && subjectId != -1L && scheduleId != -1L) {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }
                    if (subject != null && schedule != null) {
                        NotificationHelper.showAttendanceNotification(context, subject, schedule)
                        rescheduleAlarm(context, intent)
                    }
                }
            } finally {
                // Always call finish() when the work is done
                pendingResult.finish()
            }
        }
    }

    private fun rescheduleAlarm(context: Context, intent: Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val scheduleId = intent.getLongExtra("schedule_id", -1L)
        val requestCode = scheduleId.toInt()

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.WEEK_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }
}
