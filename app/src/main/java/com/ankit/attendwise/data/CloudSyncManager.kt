/*
 * Copyright (c) 2026 Ankit. All rights reserved.
 */

package com.ankit.attendwise.data

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.ankit.attendwise.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CloudSyncManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val preferencesManager = PreferencesManager(context)
    private val TAG = "CloudSyncManager"
    private var listeners = mutableListOf<ListenerRegistration>()

    private fun getUserId(): String? = auth.currentUser?.uid

    fun startRealTimeSync(dao: AttendanceDao, scope: CoroutineScope) {
        val userId = getUserId() ?: return
        stopRealTimeSync() // Clear existing

        Log.d(TAG, "Starting real-time sync for user: $userId")

        val userDoc = db.collection("users").document(userId)

        // Listen to User Profile (Name changes)
        listeners.add(userDoc.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "User profile listener error: ${e.message}")
                return@addSnapshotListener
            }
            snapshot?.let { doc ->
                val cloudName = doc.getString("name")
                if (!cloudName.isNullOrBlank()) {
                    scope.launch {
                        preferencesManager.saveUserName(cloudName)
                        Log.d(TAG, "Username updated from cloud: $cloudName")
                    }
                }
            }
        })

        // Listen to Subjects
        listeners.add(userDoc.collection("subjects").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Subjects listener error: ${e.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                for (dc in it.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            try {
                                val subject = dc.document.toObject(Subject::class.java)
                                if (subject != null) {
                                    scope.launch { dao.insertSubject(subject) }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deserializing subject: ${e.message}")
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val id = dc.document.id
                            scope.launch { dao.deleteSubjectById(id) }
                        }
                    }
                }
            }
        })

        // Listen to Schedules
        listeners.add(userDoc.collection("schedules").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Schedules listener error: ${e.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                for (dc in it.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            try {
                                val schedule = dc.document.toObject(ClassSchedule::class.java)
                                if (schedule != null) {
                                    scope.launch { dao.insertSchedule(schedule) }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deserializing schedule: ${e.message}")
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val id = dc.document.id
                            scope.launch { dao.deleteScheduleById(id) }
                        }
                    }
                }
            }
        })

        // Listen to Attendance Records
        listeners.add(userDoc.collection("attendance_records").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Records listener error: ${e.message}")
                return@addSnapshotListener
            }
            snapshot?.let {
                for (dc in it.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                            try {
                                val record = dc.document.toObject(AttendanceRecord::class.java)
                                if (record != null) {
                                    scope.launch { dao.insertAttendanceRecord(record) }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deserializing record: ${e.message}")
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val id = dc.document.id
                            scope.launch { dao.deleteAttendanceRecordById(id) }
                        }
                    }
                }
            }
        })
    }

    fun stopRealTimeSync() {
        listeners.forEach { it.remove() }
        listeners.clear()
        Log.d(TAG, "Real-time sync stopped")
    }

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

    /**
     * Checks Firestore for the latest version code.
     * Required Setup: Create collection 'app_metadata', document 'version_info', 
     * and a field 'latest_version_code' (Number).
     */
    @Keep
    data class AppUpdateInfo(
        val latestVersionCode: Int = 1,
        val isForceUpdate: Boolean = false
    )

    suspend fun getUpdateInfo(): AppUpdateInfo {
        return try {
            val doc = db.collection("app_metadata").document("version_info").get().await()
            AppUpdateInfo(
                latestVersionCode = doc.getLong("latest_version_code")?.toInt() ?: 1,
                isForceUpdate = doc.getBoolean("is_force_update") ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}")
            AppUpdateInfo() 
        }
    }

    suspend fun deleteSubject(subjectId: String) {
        val userId = getUserId() ?: return
        try {
            val userDoc = db.collection("users").document(userId)
            
            // 1. Get references to everything that needs deleting
            val subjectRef = userDoc.collection("subjects").document(subjectId)
            
            val schedules = userDoc.collection("schedules")
                .whereEqualTo("subjectId", subjectId)
                .get().await()
                
            val records = userDoc.collection("attendance_records")
                .whereEqualTo("subjectId", subjectId)
                .get().await()

            // 2. Perform atomic batch deletion with chunking for large histories
            val allRefs = mutableListOf(subjectRef)
            allRefs.addAll(schedules.documents.map { it.reference })
            allRefs.addAll(records.documents.map { it.reference })
            
            val chunks = allRefs.chunked(450)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }
            
            Log.d(TAG, "Subject, schedules, and ${records.size()} records atomically deleted from cloud: $subjectId")
        } catch (e: Exception) {
            Log.e(TAG, "Error in atomic subject deletion: ${e.message}")
        }
    }

    suspend fun syncSchedule(schedule: ClassSchedule) {
        syncSchedules(listOf(schedule))
    }

    suspend fun syncSchedules(schedules: List<ClassSchedule>) {
        val userId = getUserId() ?: return
        if (schedules.isEmpty()) return
        try {
            val userDoc = db.collection("users").document(userId)
            val chunks = schedules.chunked(450)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { schedule ->
                    val docRef = userDoc.collection("schedules").document(schedule.id)
                    batch.set(docRef, schedule, SetOptions.merge())
                }
                batch.commit().await()
            }
            Log.d(TAG, "Synced ${schedules.size} schedules in batch")
        } catch (e: Exception) {
            Log.e(TAG, "Error batch syncing schedules: ${e.message}")
        }
    }

    suspend fun deleteSchedule(scheduleId: String) {
        deleteSchedules(listOf(scheduleId))
    }

    suspend fun deleteSchedules(scheduleIds: List<String>) {
        val userId = getUserId() ?: return
        if (scheduleIds.isEmpty()) return
        try {
            val userDoc = db.collection("users").document(userId)
            val chunks = scheduleIds.chunked(450)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(userDoc.collection("schedules").document(id))
                }
                batch.commit().await()
            }
            Log.d(TAG, "Deleted ${scheduleIds.size} schedules in batch")
        } catch (e: Exception) {
            Log.e(TAG, "Error batch deleting schedules: ${e.message}")
        }
    }

    suspend fun syncAttendanceRecord(record: AttendanceRecord) {
        syncAttendanceRecords(listOf(record))
    }

    suspend fun syncAttendanceRecords(records: List<AttendanceRecord>) {
        val userId = getUserId() ?: return
        if (records.isEmpty()) return
        try {
            val userDoc = db.collection("users").document(userId)
            val chunks = records.chunked(450)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { record ->
                    val docRef = userDoc.collection("attendance_records").document(record.id)
                    batch.set(docRef, record, SetOptions.merge())
                }
                batch.commit().await()
            }
            Log.d(TAG, "Synced ${records.size} attendance records in batch")
        } catch (e: Exception) {
            Log.e(TAG, "Error batch syncing attendance records: ${e.message}")
        }
    }

    suspend fun deleteAttendanceRecord(recordId: String) {
        deleteAttendanceRecords(listOf(recordId))
    }

    suspend fun deleteAttendanceRecords(recordIds: List<String>) {
        val userId = getUserId() ?: return
        if (recordIds.isEmpty()) return
        try {
            val userDoc = db.collection("users").document(userId)
            val chunks = recordIds.chunked(450)
            for (chunk in chunks) {
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(userDoc.collection("attendance_records").document(id))
                }
                batch.commit().await()
            }
            Log.d(TAG, "Deleted ${recordIds.size} attendance records in batch")
        } catch (e: Exception) {
            Log.e(TAG, "Error batch deleting attendance records: ${e.message}")
        }
    }

    suspend fun restoreAllData(dao: AttendanceDao): Boolean {
        val userId = getUserId() ?: return false
        return try {
            Log.d(TAG, "Starting data restore for user: $userId")
            val userDoc = db.collection("users").document(userId)
            
            // 1. Restore Subjects
            val subjects = userDoc.collection("subjects").get().await().toObjects(Subject::class.java)
            Log.d(TAG, "Fetched ${subjects.size} subjects from cloud")

            // 2. Restore Schedules
            val rawSchedules = userDoc.collection("schedules").get().await().toObjects(ClassSchedule::class.java)
            
            // SANITIZATION: Only keep schedules that point to a subject we actually fetched
            val subjectIds = subjects.map { it.id }.toSet()
            val schedules = rawSchedules.filter { it.subjectId in subjectIds }
            if (rawSchedules.size != schedules.size) {
                Log.w(TAG, "Filtered out ${rawSchedules.size - schedules.size} orphaned schedules")
            }

            // 3. Restore Attendance Records
            val rawRecords = userDoc.collection("attendance_records").get().await().toObjects(AttendanceRecord::class.java)
            
            // SANITIZATION: Only keep records that point to a valid subject or are HOLIDAYs
            val records = rawRecords.filter { 
                it.subjectId == Constants.ID_SUBJECT_HOLIDAY || it.subjectId in subjectIds 
            }
            if (rawRecords.size != records.size) {
                Log.w(TAG, "Filtered out ${rawRecords.size - records.size} orphaned attendance records")
            }
            
            // Batch insert into local DB for performance and atomicity
            dao.restoreDataBatch(subjects, schedules, records)

            Log.d(TAG, "All data successfully restored and sanitized from cloud")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Critical error during data restoration: ${e.message}", e)
            false
        }
    }

    suspend fun deleteAllCloudData() {
        val userId = getUserId() ?: return
        try {
            val collections = listOf("subjects", "schedules", "attendance_records")
            
            for (collectionName in collections) {
                val snapshot = db.collection("users").document(userId)
                    .collection(collectionName).get().await()
                
                if (snapshot.isEmpty) continue
                
                // Firestore batches are limited to 500 operations
                val chunks = snapshot.documents.map { it.reference }.chunked(450)
                for (chunk in chunks) {
                    val batch = db.batch()
                    for (ref in chunk) {
                        batch.delete(ref)
                    }
                    batch.commit().await()
                }
            }

            // Delete the user profile document itself
            db.collection("users").document(userId).delete().await()

            Log.d(TAG, "All cloud data deleted for user: $userId using batches")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cloud data: ${e.message}")
        }
    }
}
