package com.ankit.smartattendance.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.data.RecordType
import com.ankit.smartattendance.utils.AlarmScheduler
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderService : Service() {
    private val TAG = "ReminderService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ReminderService started")

        val subjectId = intent?.getLongExtra("subject_id", -1L) ?: -1L
        val scheduleId = intent?.getLongExtra("schedule_id", -1L) ?: -1L

        if (subjectId != -1L && scheduleId != -1L) {
            val dao = AppDatabase.getDatabase(applicationContext).attendanceDao()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }
                    
                    val today = LocalDate.now().toEpochDay()
                    val allRecords = dao.getAllAttendanceRecords().first()
                    val isHoliday = allRecords.any { it.date == today && it.type == RecordType.HOLIDAY }
                    val isAlreadyMarked = allRecords.any { it.date == today && it.scheduleId == scheduleId && (it.type == RecordType.CLASS || it.type == RecordType.CANCELLED) }

                    if (subject != null && schedule != null) {
                        // FIX: Always reschedule for next week as soon as this alarm triggers
                        AlarmScheduler.scheduleClassAlarm(applicationContext, subject, schedule)

                        if (!isHoliday && !isAlreadyMarked) {
                            Log.d(TAG, "Found subject: ${subject.name}, creating notification")
                            val notification = NotificationHelper.buildAttendanceNotification(
                                applicationContext,
                                subject,
                                schedule
                            )
                            startForeground(schedule.id.toInt(), notification)
                            Log.d(TAG, "Foreground notification started successfully")
                        } else {
                            Log.d(TAG, "Skipping notification (holiday or already marked), but rescheduled next alarm")
                            stopSelf()
                        }
                    } else {
                        stopSelf()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in ReminderService: ${e.message}")
                    stopSelf()
                }
            }
        } else {
            Log.e(TAG, "Invalid subject or schedule ID")
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
