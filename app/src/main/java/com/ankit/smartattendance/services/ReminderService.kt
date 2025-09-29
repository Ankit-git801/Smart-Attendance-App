package com.ankit.smartattendance.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val subjectId = intent?.getLongExtra("subject_id", -1L) ?: -1L
        val scheduleId = intent?.getLongExtra("schedule_id", -1L) ?: -1L

        if (subjectId != -1L && scheduleId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getDatabase(applicationContext).attendanceDao()
                val subject = dao.getSubjectById(subjectId)
                // Using .firstOrNull() for safe access
                val schedule = dao.getSchedulesForSubject(subjectId).firstOrNull { it.id == scheduleId }

                if (subject != null && schedule != null) {
                    // DEFINITIVE FIX: Calling the new function that returns a Notification object.
                    val notification = NotificationHelper.buildAttendanceNotification(
                        applicationContext,
                        subject,
                        schedule
                    )
                    // The service now correctly starts in the foreground with the notification.
                    startForeground(schedule.id.toInt(), notification)
                } else {
                    // If subject or schedule is deleted, stop the service.
                    stopSelf()
                }
            }
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
