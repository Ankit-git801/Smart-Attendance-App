package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.utils.AlarmScheduler
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subjectName = intent.getStringExtra("subject_name")
        val subjectId = intent.getLongExtra("subject_id", -1L)
        val scheduleId = intent.getLongExtra("schedule_id", -1L)

        val pendingResult: PendingResult = goAsync()
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            try {
                if (subjectName != null && subjectId != -1L && scheduleId != -1L) {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }

                    if (subject != null && schedule != null) {
                        // Show the notification to the user
                        NotificationHelper.showAttendanceNotification(context, subject, schedule)

                        // Reschedule the alarm for the next week using the centralized scheduler
                        AlarmScheduler.scheduleClassAlarms(context, subject, listOf(schedule))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
