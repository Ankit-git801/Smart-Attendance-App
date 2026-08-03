package com.ankit.attendwise.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.attendwise.data.AppDatabase
import com.ankit.attendwise.data.RecordType
import com.ankit.attendwise.utils.Constants.ID_SCHEDULE_MANUAL
import com.ankit.attendwise.utils.NotificationHelper
import kotlinx.coroutines.*
import java.time.LocalDate

class NotificationDismissReceiver : BroadcastReceiver() {
    private val TAG = "NotificationDismiss"

    override fun onReceive(context: Context, intent: Intent) {
        val subjectId = intent.getStringExtra("subject_id") ?: ""
        val scheduleId = intent.getStringExtra("schedule_id") ?: ""

        if (subjectId.isEmpty() || scheduleId.isEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // SAFETY DELAY: Give the ActionReceiver a chance to finish database IO
                delay(500) 
                
                val dao = AppDatabase.getDatabase(context).attendanceDao()
                val today = LocalDate.now().toEpochDay()
                
                // CRITICAL: Check if it was already marked (Present/Absent/Cancelled/Holiday)
                // before re-posting. This prevents re-posting if marked just before swipe.
                val records = dao.getAttendanceRecordsForSubjectOnDate(subjectId, today)
                val isMarked = records.any { 
                    it.scheduleId == scheduleId || it.scheduleId == ID_SCHEDULE_MANUAL 
                }
                
                val todayRecords = dao.getAllAttendanceRecordsOnDateNow(today)
                val isHoliday = todayRecords.any { it.type == RecordType.HOLIDAY }

                if (!isMarked && !isHoliday) {
                    Log.d(TAG, "Notification swiped but not marked. Re-posting for $subjectId")
                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }
                    
                    if (subject != null && schedule != null) {
                        NotificationHelper.showAttendanceNotification(context, subject, schedule)
                    }
                } else {
                    Log.d(TAG, "Notification swiped but already marked or holiday. Not re-posting.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in NotificationDismissReceiver: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
