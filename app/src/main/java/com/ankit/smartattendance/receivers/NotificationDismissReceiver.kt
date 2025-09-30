package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.smartattendance.services.ReminderService

class NotificationDismissReceiver : BroadcastReceiver() {
    private val TAG = "NotificationDismiss"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notification dismissed, re-posting...")

        val subjectId = intent.getLongExtra("subject_id", -1L)
        val scheduleId = intent.getLongExtra("schedule_id", -1L)

        if (subjectId != -1L && scheduleId != -1L) {
            // Immediately restart the ReminderService to show the notification again.
            val serviceIntent = Intent(context, ReminderService::class.java).apply {
                putExtra("subject_id", subjectId)
                putExtra("schedule_id", scheduleId)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
