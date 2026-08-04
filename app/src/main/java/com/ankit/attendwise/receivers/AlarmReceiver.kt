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

        Log.d(TAG, "Received alarm for Subject ID: $subjectId, Schedule ID: $scheduleId")

        if (subjectId.isNotEmpty() && scheduleId.isNotEmpty()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    
                    // Look up subject and schedule once at the start
                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getScheduleById(scheduleId)
                    
                    if (subject == null || schedule == null) {
                        Log.e(TAG, "Subject or Schedule not found in DB. Stopping alarm chain.")
                        return@launch
                    }

                    val today = LocalDate.now().toEpochDay()
                    val todayRecords = dao.getAllAttendanceRecordsOnDateNow(today)
                    val isHoliday = todayRecords.any { it.type == RecordType.HOLIDAY }

                    // ALWAYS Reschedule for next week first to maintain the chain
                    // Use forceNextWeek = true because this alarm was just triggered for 'today'
                    AlarmScheduler.scheduleClassAlarm(context, subject, schedule, forceNextWeek = true)

                    if (isHoliday) {
                        Log.d(TAG, "Today is a Holiday. Notification skipped, but alarm rescheduled.")
                    } else {
                        // Check if already marked for today
                        val existingRecords = dao.getAttendanceRecordsForSubjectOnDate(subjectId, today)
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
