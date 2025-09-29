package com.ankit.smartattendance.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.utils.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // We only care about the BOOT_COMPLETED action
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // It's safe to launch a coroutine here to do the work off the main thread
            CoroutineScope(Dispatchers.IO).launch {
                val dao = AppDatabase.getDatabase(context).attendanceDao()

                // Get all subjects from the database
                val allSubjects = dao.getAllSubjects().first()

                // For each subject, get its schedules and reschedule the alarms
                for (subject in allSubjects) {
                    val schedules = dao.getSchedulesForSubject(subject.id)
                    if (schedules.isNotEmpty()) {
                        // DEFINITIVE FIX: Loop through each schedule and set a precise alarm.
                        schedules.forEach { schedule ->
                            AlarmScheduler.scheduleClassAlarm(context, subject, schedule)
                        }
                    }
                }
            }
        }
    }
}
