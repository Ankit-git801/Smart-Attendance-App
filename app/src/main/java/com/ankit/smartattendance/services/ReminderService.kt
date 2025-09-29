package com.ankit.smartattendance.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderService : Service() {
    private val TAG = "ReminderService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ReminderService started")

        val subjectId = intent?.getLongExtra("subject_id", -1L) ?: -1L
        val scheduleId = intent?.getLongExtra("schedule_id", -1L) ?: -1L

        Log.d(TAG, "Processing notification for Subject ID: $subjectId, Schedule ID: $scheduleId")

        if (subjectId != -1L && scheduleId != -1L) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getDatabase(applicationContext).attendanceDao()
                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }

                    if (subject != null && schedule != null) {
                        Log.d(TAG, "Found subject: ${subject.name}, creating notification")
                        val notification = NotificationHelper.buildAttendanceNotification(
                            applicationContext,
                            subject,
                            schedule
                        )
                        startForeground(schedule.id.toInt(), notification)
                        Log.d(TAG, "Foreground notification started successfully")
                    } else {
                        Log.e(TAG, "Subject or schedule not found in database")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating notification: ${e.message}")
                }
            }
        } else {
            Log.e(TAG, "Invalid subject or schedule ID")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
