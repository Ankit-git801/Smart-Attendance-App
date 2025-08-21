package com.ankit.smartattendance.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.data.AttendanceRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subjectId = intent.getLongExtra("SUBJECT_ID", -1)
        val scheduleId = intent.getLongExtra("SCHEDULE_ID", -1)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)
        val action = intent.action

        if (subjectId != -1L) {
            val dao = AppDatabase.getDatabase(context).attendanceDao()
            val isPresent = action == "MARK_PRESENT"

            CoroutineScope(Dispatchers.IO).launch {
                val record = AttendanceRecord(
                    subjectId = subjectId,
                    scheduleId = scheduleId,
                    date = LocalDate.now().toEpochDay(),
                    isPresent = isPresent,
                    note = "Marked from notification"
                )
                dao.insertAttendanceRecord(record)
            }
        }

        // Dismiss the notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
