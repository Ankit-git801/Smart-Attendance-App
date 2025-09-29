package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.data.RecordType
import com.ankit.smartattendance.utils.AlarmScheduler
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_ATTENDANCE = "com.ankit.smartattendance.ACTION_MARK_ATTENDANCE"
        const val ACTION_MARK_CANCELLED = "com.ankit.smartattendance.ACTION_MARK_CANCELLED"
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
                val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
                val isPresent = intent.getBooleanExtra(EXTRA_IS_PRESENT, false)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, 0L)

                if (subjectId != -1L) {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    val record = AttendanceRecord(
                        subjectId = subjectId,
                        scheduleId = scheduleId,
                        date = LocalDate.now().toEpochDay(),
                        isPresent = isPresent,
                        note = "Marked from notification",
                        type = RecordType.CLASS
                    )
                    dao.insertAttendanceRecord(record)

                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).firstOrNull { it.id == scheduleId }

                    if (subject != null) {
                        val total = dao.getTotalClassesForSubject(subjectId)
                        val present = dao.getPresentClassesForSubject(subjectId)
                        val newPercentage = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0

                        NotificationHelper.showUpdatedAttendanceNotification(context, subject.name, newPercentage, notificationId, false)

                        if (newPercentage < subject.targetAttendance && total > 0) {
                            NotificationHelper.showAttendanceWarningNotification(context, subject, newPercentage)
                        }

                        // DEFINITIVE FIX: Reschedule the alarm for the next week after this one is handled.
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
                val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, 0L)
                val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

                if (subjectId != -1L) {
                    val dao = AppDatabase.getDatabase(context).attendanceDao()
                    val record = AttendanceRecord(
                        subjectId = subjectId,
                        scheduleId = scheduleId,
                        date = LocalDate.now().toEpochDay(),
                        isPresent = false,
                        note = "Class Cancelled",
                        type = RecordType.CANCELLED
                    )
                    dao.insertAttendanceRecord(record)

                    val subject = dao.getSubjectById(subjectId)
                    val schedule = dao.getSchedulesForSubject(subjectId).firstOrNull { it.id == scheduleId }

                    if (subject != null) {
                        NotificationHelper.showUpdatedAttendanceNotification(context, subject.name, 0.0, notificationId, true)

                        // DEFINITIVE FIX: Reschedule the alarm for the next week even if cancelled.
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
