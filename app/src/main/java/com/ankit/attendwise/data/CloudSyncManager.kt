package com.ankit.attendwise.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class CloudSyncManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "CloudSyncManager"

    private fun getUserId(): String? = auth.currentUser?.uid

    suspend fun syncSubject(subject: Subject) {
        val userId = getUserId() ?: return
        try {
            db.collection("users").document(userId)
                .collection("subjects").document(subject.id)
                .set(subject, SetOptions.merge())
                .await()
            Log.d(TAG, "Subject synced: ${subject.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing subject: ${e.message}")
        }
    }

    suspend fun syncUserProfile(name: String) {
        val userId = getUserId() ?: return
        try {
            val data = mapOf(
                "name" to name,
                "email" to (auth.currentUser?.email ?: ""),
                "lastUpdated" to System.currentTimeMillis()
            )
            db.collection("users").document(userId)
                .set(data, SetOptions.merge())
                .await()
            Log.d(TAG, "User profile synced: $name")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing user profile: ${e.message}")
        }
    }

    suspend fun getUserProfileName(): String? {
        val userId = getUserId() ?: return null
        return try {
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteSubject(subjectId: String) {
        val userId = getUserId() ?: return
        try {
            db.collection("users").document(userId)
                .collection("subjects").document(subjectId)
                .delete()
                .await()
            Log.d(TAG, "Subject deleted from cloud: $subjectId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting subject from cloud: ${e.message}")
        }
    }

    suspend fun syncSchedule(schedule: ClassSchedule) {
        val userId = getUserId() ?: return
        try {
            db.collection("users").document(userId)
                .collection("schedules").document(schedule.id)
                .set(schedule, SetOptions.merge())
                .await()
            Log.d(TAG, "Schedule synced: ${schedule.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing schedule: ${e.message}")
        }
    }

    suspend fun deleteSchedule(scheduleId: String) {
        val userId = getUserId() ?: return
        try {
            db.collection("users").document(userId)
                .collection("schedules").document(scheduleId)
                .delete()
                .await()
            Log.d(TAG, "Schedule deleted from cloud: $scheduleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting schedule from cloud: ${e.message}")
        }
    }

    suspend fun syncAttendanceRecord(record: AttendanceRecord) {
        val userId = getUserId() ?: return
        try {
            db.collection("users").document(userId)
                .collection("attendance_records").document(record.id)
                .set(record, SetOptions.merge())
                .await()
            Log.d(TAG, "Attendance record synced: ${record.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing attendance record: ${e.message}")
        }
    }

    suspend fun deleteAttendanceRecord(recordId: String) {
        val userId = getUserId() ?: return
        try {
            db.collection("users").document(userId)
                .collection("attendance_records").document(recordId)
                .delete()
                .await()
            Log.d(TAG, "Attendance record deleted from cloud: $recordId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting attendance record from cloud: ${e.message}")
        }
    }

    suspend fun restoreAllData(dao: AttendanceDao): Boolean {
        val userId = getUserId() ?: return false
        return try {
            Log.d(TAG, "Starting data restore for user: $userId")
            
            // 1. Restore Subjects
            val subjects = db.collection("users").document(userId)
                .collection("subjects").get().await().toObjects(Subject::class.java)
            
            Log.d(TAG, "Fetched ${subjects.size} subjects from cloud")
            for (subject in subjects) {
                dao.insertSubject(subject)
            }

            // 2. Restore Schedules
            val schedules = db.collection("users").document(userId)
                .collection("schedules").get().await().toObjects(ClassSchedule::class.java)
            
            Log.d(TAG, "Fetched ${schedules.size} schedules from cloud")
            for (schedule in schedules) {
                dao.insertSchedule(schedule)
            }

            // 3. Restore Attendance Records
            val records = db.collection("users").document(userId)
                .collection("attendance_records").get().await().toObjects(AttendanceRecord::class.java)
            
            Log.d(TAG, "Fetched ${records.size} attendance records from cloud")
            for (record in records) {
                dao.insertAttendanceRecord(record)
            }

            Log.d(TAG, "All data successfully restored from cloud")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring data: ${e.message}", e)
            false
        }
    }

    suspend fun deleteAllCloudData() {
        val userId = getUserId() ?: return
        try {
            // Delete all collections under the user
            val subjects = db.collection("users").document(userId).collection("subjects").get().await()
            for (doc in subjects.documents) doc.reference.delete().await()

            val schedules = db.collection("users").document(userId).collection("schedules").get().await()
            for (doc in schedules.documents) doc.reference.delete().await()

            val records = db.collection("users").document(userId).collection("attendance_records").get().await()
            for (doc in records.documents) doc.reference.delete().await()

            // Delete the user profile document itself
            db.collection("users").document(userId).delete().await()

            Log.d(TAG, "All cloud data deleted for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cloud data: ${e.message}")
        }
    }
}
