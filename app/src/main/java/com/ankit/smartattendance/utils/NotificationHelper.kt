package com.ankit.smartattendance.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ankit.smartattendance.MainActivity
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.receivers.NotificationActionReceiver

object NotificationHelper {

    private const val CHANNEL_ID = "attendance_channel"
    private const val WARNING_CHANNEL_ID = "warning_channel"
    private const val LOW_ATTENDANCE_NOTIFICATION_ID = -1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Attendance Reminders"
            val descriptionText = "Notifications to remind you to mark your attendance."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val warningName = "Low Attendance Warnings"
            val warningDescription = "Notifications for when your attendance drops below the target."
            val warningImportance = NotificationManager.IMPORTANCE_HIGH
            val warningChannel = NotificationChannel(WARNING_CHANNEL_ID, warningName, warningImportance).apply {
                description = warningDescription
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }

    // DEFINITIVE FIX: This function now BUILDS and RETURNS the notification for the service.
    fun buildAttendanceNotification(context: Context, subject: Subject, schedule: ClassSchedule): Notification {
        val notificationId = schedule.id.toInt()

        val presentIntent = createActionIntent(context, subject.id, schedule.id, notificationId, true)
        val absentIntent = createActionIntent(context, subject.id, schedule.id, notificationId, false)
        val cancelIntent = createCancelActionIntent(context, subject.id, schedule.id, notificationId)

        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingMainIntent = PendingIntent.getActivity(context, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using a guaranteed system icon
            .setContentTitle("Attendance for ${subject.name}")
            .setContentText("Did you attend the class?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingMainIntent)
            .addAction(0, "Present", presentIntent)
            .addAction(0, "Absent", absentIntent)
            .addAction(0, "Cancel", cancelIntent)
            .setOngoing(true) // Makes the notification persistent for a foreground service
            .build()
    }

    fun showUpdatedAttendanceNotification(context: Context, subjectName: String, newPercentage: Double, notificationId: Int, wasCancelled: Boolean) {
        val message = if (wasCancelled) {
            "Class Cancelled."
        } else {
            "Attendance marked. New percentage: ${"%.2f".format(newPercentage)}%"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(subjectName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setTimeoutAfter(5000)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }

    fun showAttendanceWarningNotification(context: Context, subject: Subject, newPercentage: Double) {
        val builder = NotificationCompat.Builder(context, WARNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Using a guaranteed system icon
            .setContentTitle("Low Attendance Warning")
            .setContentText("Your attendance for ${subject.name} has dropped to ${"%.2f".format(newPercentage)}%.")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Your attendance for ${subject.name} is ${"%.2f".format(newPercentage)}%, which is below your target of ${subject.targetAttendance}%."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(LOW_ATTENDANCE_NOTIFICATION_ID, builder.build())
    }

    private fun createActionIntent(context: Context, subjectId: Long, scheduleId: Long, notificationId: Int, isPresent: Boolean): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_ATTENDANCE
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subjectId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(NotificationActionReceiver.EXTRA_IS_PRESENT, isPresent)
        }
        return PendingIntent.getBroadcast(context, (notificationId * 10) + if (isPresent) 1 else 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createCancelActionIntent(context: Context, subjectId: Long, scheduleId: Long, notificationId: Int): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_CANCELLED
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subjectId)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        return PendingIntent.getBroadcast(context, notificationId * 10 + 3, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
