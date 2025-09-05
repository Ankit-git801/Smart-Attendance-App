package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.services.ReminderService
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_ATTENDANCE = "com.ankit.smartattendance.ACTION_MARK_ATTENDANCE"
        const val EXTRA_SUBJECT_ID = "EXTRA_SUBJECT_ID"
        const val EXTRA_IS_PRESENT = "EXTRA_IS_PRESENT"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_ATTENDANCE) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
                    val isPresent = intent.getBooleanExtra(EXTRA_IS_PRESENT, false)
                    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
                    val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)

                    // Stop the foreground service
                    context.stopService(Intent(context, ReminderService::class.java))

                    if (subjectId != -1L) {
                        val dao = AppDatabase.getDatabase(context).attendanceDao()
                        val today = LocalDate.now().toEpochDay()

                        // **FIX**: Delete any previous record for this subject today before inserting a new one.
                        dao.deleteRecordsForSubjectOnDate(subjectId, today)

                        val record = AttendanceRecord(
                            subjectId = subjectId,
                            scheduleId = if (scheduleId != -1L) scheduleId else null,
                            date = today,
                            isPresent = isPresent,
                            note = "Marked from notification"
                        )
                        dao.insertAttendanceRecord(record)

                        // Update the user with a confirmation notification
                        val subject = dao.getSubjectById(subjectId)
                        if (subject != null) {
                            val total = dao.getTotalClassesForSubject(subjectId)
                            val present = dao.getPresentClassesForSubject(subjectId)
                            val newPercentage = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0

                            NotificationHelper.showUpdatedAttendanceNotification(
                                context,
                                subject.name,
                                newPercentage,
                                notificationId
                            )

                            if (newPercentage < subject.targetAttendance) {
                                NotificationHelper.showAttendanceWarningNotification(
                                    context,
                                    subject,
                                    newPercentage
                                )
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
