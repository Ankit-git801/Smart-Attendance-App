package com.ankit.attendwise.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.attendwise.services.ReminderService

class AlarmReceiver : BroadcastReceiver() {
    private val TAG = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver triggered!")

        val subjectId = intent.getStringExtra("subject_id") ?: ""
        val scheduleId = intent.getStringExtra("schedule_id") ?: ""
        val subjectName = intent.getStringExtra("subject_name") ?: "Unknown"

        Log.d(TAG, "Received alarm for: $subjectName (Subject ID: $subjectId, Schedule ID: $scheduleId)")

        if (subjectId.isNotEmpty() && scheduleId.isNotEmpty()) {
            val serviceIntent = Intent(context, ReminderService::class.java).apply {
                putExtra("subject_id", subjectId)
                putExtra("schedule_id", scheduleId)
            }

            try {
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "ReminderService started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ReminderService: ${e.message}")
            }
        } else {
            Log.e(TAG, "Invalid subject or schedule ID received")
        }
    }
}
