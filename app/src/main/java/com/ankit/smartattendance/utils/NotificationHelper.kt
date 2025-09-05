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
import com.ankit.smartattendance.R
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.receivers.NotificationActionReceiver
import com.ankit.smartattendance.receivers.NotificationDismissReceiver

object NotificationHelper {

    private const val CHANNEL_ID = "attendance_channel_v2"
    private const val CHANNEL_NAME = "Attendance Reminders"
    private const val WARNING_CHANNEL_ID = "attendance_warning_channel_v2"
    private const val WARNING_CHANNEL_NAME = "Attendance Warnings"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "High-priority reminders for upcoming classes." }

            val warningChannel = NotificationChannel(
                WARNING_CHANNEL_ID,
                WARNING_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Alerts for low attendance." }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }

    fun getAttendanceNotification(context: Context, subject: Subject, schedule: ClassSchedule): Notification {
        val notificationId = schedule.id.toInt()

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPendingIntent = PendingIntent.getActivity(context, notificationId, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Present Intent
        val presentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_ATTENDANCE
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subject.id)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(NotificationActionReceiver.EXTRA_IS_PRESENT, true)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val presentPendingIntent = PendingIntent.getBroadcast(context, notificationId * 10 + 1, presentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Absent Intent
        val absentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_ATTENDANCE
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subject.id)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(NotificationActionReceiver.EXTRA_IS_PRESENT, false)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val absentPendingIntent = PendingIntent.getBroadcast(context, notificationId * 10 + 2, absentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // THIS IS THE FIX: Create the intent for the Cancelled action.
        val cancelledIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_CANCELLED
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subject.id)
            putExtra(NotificationActionReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val cancelledPendingIntent = PendingIntent.getBroadcast(context, notificationId * 10 + 3, cancelledIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val deleteIntent = Intent(context, NotificationDismissReceiver::class.java).apply {
            putExtra("subject_id", subject.id)
            putExtra("schedule_id", schedule.id)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(context, notificationId * 10 + 4, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(subject.name)
            .setContentText("Did you attend the class?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(contentPendingIntent, true)
            // THIS IS THE FIX: We now reference the correct PNG drawables.
            .addAction(R.drawable.ic_action_present, "Present", presentPendingIntent)
            .addAction(R.drawable.ic_action_absent, "Absent", absentPendingIntent)
            .addAction(R.drawable.ic_action_cancelled, "Cancel", cancelledPendingIntent)
            .setOngoing(true)
            .setDeleteIntent(deletePendingIntent)
            .build()
    }

    fun showUpdatedAttendanceNotification(context: Context, subjectName: String, newPercentage: Double, notificationId: Int, wasCancelled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val title = if (wasCancelled) "$subjectName Class Cancelled" else "$subjectName Attendance Updated"
        val text = if (wasCancelled) "The class has been marked as cancelled." else "Your new attendance is ${"%.1f".format(newPercentage)}%."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    fun showAttendanceWarningNotification(context: Context, subject: Subject, currentPercentage: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val warningNotificationId = subject.id.toInt() + 10000
        val builder = NotificationCompat.Builder(context, WARNING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Low Attendance Alert!")
            .setContentText("Your attendance for ${subject.name} is ${"%.1f".format(currentPercentage)}%, which is below your target of ${subject.targetAttendance}%.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(warningNotificationId, builder.build())
    }
}
