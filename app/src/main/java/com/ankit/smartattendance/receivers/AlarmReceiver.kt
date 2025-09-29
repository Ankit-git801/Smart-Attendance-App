package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.services.ReminderService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This receiver's only job is to catch the alarm and start the service.
        val subjectId = intent.getLongExtra("subject_id", -1L)
        val scheduleId = intent.getLongExtra("schedule_id", -1L)

        if (subjectId != -1L && scheduleId != -1L) {
            // Create an intent for the ReminderService.
            val serviceIntent = Intent(context, ReminderService::class.java).apply {
                // Pass the IDs directly to the service.
                putExtra("subject_id", subjectId)
                putExtra("schedule_id", scheduleId)
            }
            // Start the service in the foreground. This is crucial for reliability on modern Android.
            context.startForegroundService(serviceIntent)
        }
    }
}
