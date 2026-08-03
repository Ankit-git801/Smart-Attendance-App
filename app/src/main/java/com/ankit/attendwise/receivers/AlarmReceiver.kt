package com.ankit.attendwise.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.attendwise.data.AppDatabase
import com.ankit.attendwise.data.RecordType
import com.ankit.attendwise.utils.AlarmScheduler
import com.ankit.attendwise.utils.Constants.ID_SCHEDULE_MANUAL
import com.ankit.attendwise.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    private val TAG = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver triggered!")

        val subjectId = intent.getStringExtra("subject_id") ?: ""
        val scheduleId = intent.getStringExtra("schedule_id") ?: ""
        val subjectName = intent.getStringExtra("subject_name") ?: "Unknown"

        Log.d(TAG, "Received alarm for: $subjectName (Subject ID: $subjectId, Schedule ID: $scheduleId)")

        if (subjectId.isNotEmpty() && scheduleId.isNotEmpty()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    val today = LocalDate.now().toEpochDay()
                    val todayRecords = dao.getAllAttendanceRecordsOnDateNow(today)
                    val isHoliday = todayRecords.any { it.type == RecordType.HOLIDAY }

                    if (isHoliday) {
                        Log.d(TAG, "Today is a Holiday. Rescheduling for next week without showing notification.")
                        val subject = dao.getSubjectById(subjectId)
                        val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }
                        if (subject != null && schedule != null) {
                            AlarmScheduler.scheduleClassAlarm(context, subject, schedule)
                        }
                    } else {
                        val subject = dao.getSubjectById(subjectId)
                        val schedule = dao.getSchedulesForSubject(subjectId).find { it.id == scheduleId }
                        
                        if (subject != null && schedule != null) {
                            // Reschedule for next week
                            AlarmScheduler.scheduleClassAlarm(context, subject, schedule)

                            // Check if already marked for today
                            val dateAsLong = LocalDate.now().toEpochDay()
                            val existingRecords = dao.getAttendanceRecordsForSubjectOnDate(subjectId, dateAsLong)
                            val isAlreadyMarked = existingRecords.any { 
                                it.scheduleId == scheduleId || it.scheduleId == ID_SCHEDULE_MANUAL
                            }

                            if (!isAlreadyMarked) {
                                Log.d(TAG, "Showing notification for ${subject.name ?: "Class"}")
                                NotificationHelper.showAttendanceNotification(context, subject, schedule)
                            } else {
                                Log.d(TAG, "Skipping notification (already marked)")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in AlarmReceiver: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        } else {
            Log.e(TAG, "Invalid subject or schedule ID received")
        }
    }
}
