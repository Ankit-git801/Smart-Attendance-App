package com.ankit.attendwise.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.attendwise.services.ReminderService

class NotificationDismissReceiver : BroadcastReceiver() {
    private val TAG = "NotificationDismiss"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notification dismissed, re-posting...")

        val subjectId = intent.getStringExtra("subject_id") ?: ""
        val scheduleId = intent.getStringExtra("schedule_id") ?: ""

        if (subjectId.isNotEmpty() && scheduleId.isNotEmpty()) {
            // Immediately restart the ReminderService to show the notification again.
            val serviceIntent = Intent(context, ReminderService::class.java).apply {
                putExtra("subject_id", subjectId)
                putExtra("schedule_id", scheduleId)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
