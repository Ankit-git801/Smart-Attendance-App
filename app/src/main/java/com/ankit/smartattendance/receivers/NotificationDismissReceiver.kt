package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.services.ReminderService

class NotificationDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // This receiver is triggered when the notification is swiped away.
        // We re-launch the service to make the notification reappear.
        val subjectId = intent.getLongExtra("subject_id", -1L)
        val scheduleId = intent.getLongExtra("schedule_id", -1L)

        if (subjectId != -1L && scheduleId != -1L) {
            val serviceIntent = Intent(context, ReminderService::class.java).apply {
                putExtra("subject_id", subjectId)
                putExtra("schedule_id", scheduleId)
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
