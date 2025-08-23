package com.ankit.smartattendance.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.receivers.NotificationActionReceiver

object NotificationHelper {

    private const val CHANNEL_ID = "attendance_channel"
    private const val CHANNEL_NAME = "Attendance Reminders"
    private const val WARNING_CHANNEL_ID = "attendance_warning_channel"
    private const val WARNING_CHANNEL_NAME = "Attendance Warnings"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            val warningChannel = NotificationChannel(WARNING_CHANNEL_ID, WARNING_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }

    fun showAttendanceNotification(context: Context, subject: Subject, schedule: ClassSchedule) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = schedule.id.toInt()

        // Use distinct request codes for actions to avoid collision with the main alarm
        val presentRequestCode = notificationId + 20000
        val absentRequestCode = notificationId + 30000

        val presentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_ATTENDANCE
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subject.id)
            putExtra(NotificationActionReceiver.EXTRA_IS_PRESENT, true)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val presentPendingIntent = PendingIntent.getBroadcast(context, presentRequestCode, presentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val absentIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_ATTENDANCE
            putExtra(NotificationActionReceiver.EXTRA_SUBJECT_ID, subject.id)
            putExtra(NotificationActionReceiver.EXTRA_IS_PRESENT, false)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val absentPendingIntent = PendingIntent.getBroadcast(context, absentRequestCode, absentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(subject.name)
            .setContentText("Did you attend the class?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Present", presentPendingIntent)
            .addAction(0, "Absent", absentPendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    fun showUpdatedAttendanceNotification(context: Context, subjectName: String, newPercentage: Double, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$subjectName Attendance Updated")
            .setContentText("Your new attendance is ${"%.1f".format(newPercentage)}%.")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }

    fun showAttendanceWarningNotification(context: Context, subject: Subject, currentPercentage: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val warningNotificationId = subject.id.toInt() + 10000
        val builder = NotificationCompat.Builder(context, WARNING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Low Attendance Alert!")
            .setContentText("Your attendance for ${subject.name} is ${"%.1f".format(currentPercentage)}%, which is below your target of ${subject.targetAttendance}%.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Your attendance for ${subject.name} is ${"%.1f".format(currentPercentage)}%, which is below your target of ${subject.targetAttendance}%. Take action to improve it."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(warningNotificationId, builder.build())
    }
}
