package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.smartattendance.services.ReminderService

class AlarmReceiver : BroadcastReceiver() {
    private val TAG = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "AlarmReceiver triggered!")

        val subjectId = intent.getLongExtra("subject_id", -1L)
        val scheduleId = intent.getLongExtra("schedule_id", -1L)
        val subjectName = intent.getStringExtra("subject_name") ?: "Unknown"

        Log.d(TAG, "Received alarm for: $subjectName (Subject ID: $subjectId, Schedule ID: $scheduleId)")

        if (subjectId != -1L && scheduleId != -1L) {
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
