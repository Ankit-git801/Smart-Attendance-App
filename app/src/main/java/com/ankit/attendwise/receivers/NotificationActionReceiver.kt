package com.ankit.attendwise.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ankit.attendwise.data.*
import com.ankit.attendwise.utils.Constants.ID_SCHEDULE_MANUAL
import com.ankit.attendwise.utils.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.util.UUID

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        private val actionMutex = Mutex() // Global safety lock for notification actions
        const val ACTION_MARK_ATTENDANCE = "com.ankit.attendwise.ACTION_MARK_ATTENDANCE"
        const val ACTION_MARK_CANCELLED = "com.ankit.attendwise.ACTION_MARK_CANCELLED"
        const val EXTRA_SUBJECT_ID = "EXTRA_SUBJECT_ID"
        const val EXTRA_IS_PRESENT = "EXTRA_IS_PRESENT"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
        const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MARK_ATTENDANCE -> handleMarkAttendance(context, intent)
            ACTION_MARK_CANCELLED -> handleMarkCancelled(context, intent)
        }
    }

    private fun handleMarkAttendance(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                actionMutex.withLock {
                    val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: ""
                    val isPresent = intent.getBooleanExtra(EXTRA_IS_PRESENT, false)
                    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
                    val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: ""

                    if (subjectId.isNotEmpty()) {
                        val dao = AppDatabase.getDatabase(context).attendanceDao()
                        val cloudSyncManager = CloudSyncManager(context)
                        val today = LocalDate.now().toEpochDay()

                        val allDayRecords = dao.getAllAttendanceRecordsOnDateNow(today)
                        if (allDayRecords.any { it.type == RecordType.HOLIDAY }) {
                            NotificationHelper.cancelNotification(context, notificationId)
                            return@withLock
                        }

                        val existingRecords = dao.getAttendanceRecordsForSubjectOnDate(subjectId, today)
                        val recordIdsToClean = existingRecords.filter { (it.scheduleId == scheduleId) || (it.scheduleId == ID_SCHEDULE_MANUAL) }.map { it.id }

                        val record = AttendanceRecord(
                            id = UUID.randomUUID().toString(),
                            subjectId = subjectId,
                            scheduleId = scheduleId,
                            date = today,
                            isPresent = isPresent,
                            note = "Marked from notification",
                            type = RecordType.CLASS,
                        )
                        
                        dao.markAttendanceTransaction(recordIdsToClean, record)
                        
                        // SEQUENTIAL SYNC: Ensure cloud backup is complete before receiver finishes
                        if (recordIdsToClean.isNotEmpty()) {
                            cloudSyncManager.deleteAttendanceRecords(recordIdsToClean)
                        }
                        cloudSyncManager.syncAttendanceRecord(record)

                        val subject = dao.getSubjectById(subjectId)

                        if (subject != null) {
                            val total = dao.getTotalClassesForSubject(subjectId)
                            val present = dao.getPresentClassesForSubject(subjectId)
                            val newPercentage = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0

                            NotificationHelper.showUpdatedAttendanceNotification(context, subject.name, newPercentage, notificationId, false)

                            if (newPercentage < subject.targetAttendance && total > 0) {
                                NotificationHelper.showAttendanceWarningNotification(context, subject, newPercentage)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error marking attendance: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleMarkCancelled(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                actionMutex.withLock {
                    val subjectId = intent.getStringExtra(EXTRA_SUBJECT_ID) ?: ""
                    val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: ""
                    val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)

                    if (subjectId.isNotEmpty()) {
                        val dao = AppDatabase.getDatabase(context).attendanceDao()
                        val cloudSyncManager = CloudSyncManager(context)
                        val today = LocalDate.now().toEpochDay()

                        val allDayRecords = dao.getAllAttendanceRecordsOnDateNow(today)
                        if (allDayRecords.any { it.type == RecordType.HOLIDAY }) {
                            NotificationHelper.cancelNotification(context, notificationId)
                            return@withLock
                        }

                        val existingRecords = dao.getAttendanceRecordsForSubjectOnDate(subjectId, today)
                        val recordIdsToClean = existingRecords.filter { (it.scheduleId == scheduleId) || (it.scheduleId == ID_SCHEDULE_MANUAL) }.map { it.id }

                        val record = AttendanceRecord(
                            id = UUID.randomUUID().toString(),
                            subjectId = subjectId,
                            scheduleId = scheduleId,
                            date = today,
                            isPresent = false,
                            note = "Class Cancelled",
                            type = RecordType.CANCELLED,
                        )
                        
                        dao.markAttendanceTransaction(recordIdsToClean, record)
                        
                        // SEQUENTIAL SYNC: Ensure cloud backup is complete before receiver finishes
                        if (recordIdsToClean.isNotEmpty()) {
                            cloudSyncManager.deleteAttendanceRecords(recordIdsToClean)
                        }
                        cloudSyncManager.syncAttendanceRecord(record)

                        val subject = dao.getSubjectById(subjectId)

                        if (subject != null) {
                            val total = dao.getTotalClassesForSubject(subjectId)
                            val present = dao.getPresentClassesForSubject(subjectId)
                            val newPercentage = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0
                            
                            NotificationHelper.showUpdatedAttendanceNotification(context, subject.name, newPercentage, notificationId, true)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error marking cancelled: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
