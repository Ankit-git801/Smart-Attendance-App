package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_ATTENDANCE = "com.ankit.smartattendance.ACTION_MARK_ATTENDANCE"
        const val EXTRA_SUBJECT_ID = "EXTRA_SUBJECT_ID"
        const val EXTRA_IS_PRESENT = "EXTRA_IS_PRESENT"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MARK_ATTENDANCE) {
            val subjectId = intent.getLongExtra(EXTRA_SUBJECT_ID, -1L)
            val isPresent = intent.getBooleanExtra(EXTRA_IS_PRESENT, false)
            val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

            if (subjectId != -1L) {
                val dao = AppDatabase.getDatabase(context).attendanceDao()

                GlobalScope.launch {
                    // 1. Insert the new attendance record
                    val record = AttendanceRecord(
                        subjectId = subjectId,
                        scheduleId = null,
                        date = LocalDate.now().toEpochDay(),
                        isPresent = isPresent,
                        note = "Marked from notification"
                    )
                    dao.insertAttendanceRecord(record)

                    // 2. Fetch the subject and calculate the new percentage
                    val subject = dao.getSubjectById(subjectId)
                    if (subject != null) {
                        val total = dao.getTotalClassesForSubject(subjectId)
                        val present = dao.getPresentClassesForSubject(subjectId)
                        val newPercentage = if (total > 0) (present.toDouble() / total) * 100 else 100.0

                        // 3. Show the updated attendance status notification
                        NotificationHelper.showUpdatedAttendanceNotification(
                            context,
                            subject.name,
                            newPercentage,
                            notificationId
                        )

                        // 4. CHECK IF ATTENDANCE IS BELOW TARGET
                        if (newPercentage < subject.targetAttendance) {
                            NotificationHelper.showAttendanceWarningNotification(
                                context,
                                subject,
                                newPercentage
                            )
                        }
                    }
                }
            }
        }
    }
}
