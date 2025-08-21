package com.ankit.smartattendance.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ankit.smartattendance.MainActivity
import com.ankit.smartattendance.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val subjectName = intent.getStringExtra("SUBJECT_NAME") ?: "Class"
        val subjectId = intent.getLongExtra("SUBJECT_ID", -1)
        val scheduleId = intent.getLongExtra("SCHEDULE_ID", -1)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("class_reminders", "Class Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, openAppIntent, PendingIntent.FLAG_IMMUTABLE)

        // Intent for "Present" action
        val presentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "MARK_PRESENT"
            putExtra("SUBJECT_ID", subjectId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val presentPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, notificationId * 10 + 1, presentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Intent for "Absent" action
        val absentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "MARK_ABSENT"
            putExtra("SUBJECT_ID", subjectId)
            putExtra("SCHEDULE_ID", scheduleId)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val absentPendingIntent: PendingIntent = PendingIntent.getBroadcast(context, notificationId * 10 + 2, absentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Build the notification
        val builder = NotificationCompat.Builder(context, "class_reminders")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure you have this drawable
            .setContentTitle("Class Reminder")
            .setContentText("$subjectName is starting now. Mark your attendance.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Present", presentPendingIntent)
            .addAction(R.drawable.ic_launcher_foreground, "Absent", absentPendingIntent)

        notificationManager.notify(notificationId, builder.build())
    }
}
