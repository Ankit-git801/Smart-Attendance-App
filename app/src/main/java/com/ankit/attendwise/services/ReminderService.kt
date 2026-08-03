package com.ankit.attendwise.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.ankit.attendwise.data.AppDatabase
import com.ankit.attendwise.data.RecordType
import com.ankit.attendwise.utils.AlarmScheduler
import com.ankit.attendwise.utils.Constants.ID_SCHEDULE_MANUAL
import com.ankit.attendwise.utils.NotificationHelper
import kotlinx.coroutines.*
import java.time.LocalDate

class ReminderService : Service() {
    private val TAG = "ReminderService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ReminderService started")

        val subjectId = intent?.getStringExtra("subject_id") ?: ""
        val scheduleId = intent?.getStringExtra("schedule_id") ?: ""

        if (subjectId.isNotEmpty() && scheduleId.isNotEmpty()) {
            val dao = AppDatabase.getDatabase(applicationContext).attendanceDao()
            
            serviceScope.launch {
                try {
                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }
                    
                    if (subject != null && schedule != null) {
                        // Reschedule for next week
                        AlarmScheduler.scheduleClassAlarm(applicationContext, subject, schedule)

                        // Check if we should show notification
                        val today = LocalDate.now().toEpochDay()
                        val todayRecords = dao.getAllAttendanceRecordsOnDateNow(today)
                        val isAlreadyMarked = todayRecords.any { 
                            it.subjectId == subjectId && 
                            (it.scheduleId == scheduleId || it.scheduleId == ID_SCHEDULE_MANUAL) && 
                            (it.type == RecordType.CLASS || it.type == RecordType.CANCELLED || it.type == RecordType.MANUAL) 
                        }

                        if (!isAlreadyMarked) {
                            Log.d(TAG, "Creating notification for ${subject.name}")
                            val notification = NotificationHelper.buildAttendanceNotification(
                                applicationContext,
                                subject,
                                schedule
                            )
                            // Call startForeground to satisfy system requirements
                            startForeground(schedule.id.hashCode(), notification)
                        } else {
                            Log.d(TAG, "Skipping notification (already marked)")
                            // Even if skipping, if we were started with startForegroundService, 
                            // we MUST call startForeground or the app will crash.
                            
                            // To be absolutely safe, we show the notification and then immediately stop
                            val notification = NotificationHelper.buildAttendanceNotification(applicationContext, subject, schedule)
                            startForeground(schedule.id.hashCode(), notification)
                            stopForeground(STOP_FOREGROUND_REMOVE)
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

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
