package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Here, you would re-schedule all your alarms.
            // This requires an AlarmScheduler utility class, which we can add if you want this feature fully implemented.
            // For now, this is a placeholder to complete the project structure.
        }
    }
}
