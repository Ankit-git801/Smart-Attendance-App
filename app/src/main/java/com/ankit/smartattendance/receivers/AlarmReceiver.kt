package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.services.ReminderService
import com.ankit.smartattendance.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subjectId = intent.getLongExtra("subject_id", -1L)
        val scheduleId = intent.getLongExtra("schedule_id", -1L)

        val pendingResult: PendingResult = goAsync()
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            try {
                if (subjectId != -1L && scheduleId != -1L) {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }

                    if (subject != null && schedule != null) {
                        // Start the foreground service to show the notification
                        val serviceIntent = Intent(context, ReminderService::class.java).apply {
                            putExtra("subject_id", subject.id)
                            putExtra("schedule_id", schedule.id)
                        }
                        context.startForegroundService(serviceIntent)

                        // Reschedule the alarm for the next week
                        AlarmScheduler.scheduleClassAlarms(context, subject, listOf(schedule))
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
