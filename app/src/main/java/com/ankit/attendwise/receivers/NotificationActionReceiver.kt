package com.ankit.attendwise.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.attendwise.data.*
import com.ankit.attendwise.utils.AlarmScheduler
import com.ankit.attendwise.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_ATTENDANCE = "com.ankit.attendwise.ACTION_MARK_ATTENDANCE"
        const val ACTION_MARK_CANCELLED = "com.ankit.attendwise.ACTION_MARK_CANCELLED"
        const val EXTRA_SUBJECT_ID = "EXTRA_SUBJECT_ID"
        const val EXTRA_IS_PRESENT = "EXTRA_IS_PRESENT"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_ATTENDANCE -> handleMarkAttendance(context, intent)
            ACTION_MARK_CANCELLED -> handleMarkCancelled(context, intent)
        }
    }

    private fun handleMarkAttendance(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: ""
                val isPresent = intent.getBooleanExtra(EXTRA_IS_PRESENT, false)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
                val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: ""

                if (subjectId.isNotEmpty()) {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    val cloudSyncManager = CloudSyncManager(context)
                    val today = LocalDate.now().toEpochDay()

                    // Clean up existing records for this specific slot to prevent duplicates
                    val existingRecords = dao.getAttendanceRecordsForSubjectOnDate(subjectId, today)
                    existingRecords.filter { it.scheduleId == scheduleId || it.scheduleId == "-1" }.forEach { record ->
                        dao.deleteAttendanceRecord(record)
                        cloudSyncManager.deleteAttendanceRecord(record.id)
                    }

                    val record = AttendanceRecord(
                        id = java.util.UUID.randomUUID().toString(),
                        subjectId = subjectId,
                        scheduleId = scheduleId,
                        date = today,
                        isPresent = isPresent,
                        note = "Marked from notification",
                        type = RecordType.CLASS
                    )
                    dao.insertAttendanceRecord(record)
                    cloudSyncManager.syncAttendanceRecord(record)

                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).firstOrNull { it.id == scheduleId }

                    if (subject != null) {
                        val total = dao.getTotalClassesForSubject(subjectId)
                        val present = dao.getPresentClassesForSubject(subjectId)
                        val newPercentage = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0

                        // This will now resolve correctly.
                        NotificationHelper.showUpdatedAttendanceNotification(context, subject.name, newPercentage, notificationId, false)

                        if (newPercentage < subject.targetAttendance && total > 0) {
                            // This will now resolve correctly.
                            NotificationHelper.showAttendanceWarningNotification(context, subject, newPercentage)
                        }

                        if (schedule != null) {
                            AlarmScheduler.scheduleClassAlarm(context, subject, schedule)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleMarkCancelled(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: ""
                val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: ""
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

                if (subjectId.isNotEmpty()) {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    val cloudSyncManager = CloudSyncManager(context)
                    val today = LocalDate.now().toEpochDay()

                    // Clean up existing records for this specific slot to prevent duplicates
                    val existingRecords = dao.getAttendanceRecordsForSubjectOnDate(subjectId, today)
                    existingRecords.filter { it.scheduleId == scheduleId || it.scheduleId == "-1" }.forEach { record ->
                        dao.deleteAttendanceRecord(record)
                        cloudSyncManager.deleteAttendanceRecord(record.id)
                    }

                    val record = AttendanceRecord(
                        id = java.util.UUID.randomUUID().toString(),
                        subjectId = subjectId,
                        scheduleId = scheduleId,
                        date = today,
                        isPresent = false,
                        note = "Class Cancelled",
                        type = RecordType.CANCELLED
                    )
                    dao.insertAttendanceRecord(record)
                    cloudSyncManager.syncAttendanceRecord(record)

                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).firstOrNull { it.id == scheduleId }

                    if (subject != null) {
                        // This will now resolve correctly.
                        NotificationHelper.showUpdatedAttendanceNotification(context, subject.name, 0.0, notificationId, true)

                        if (schedule != null) {
                            AlarmScheduler.scheduleClassAlarm(context, subject, schedule)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
