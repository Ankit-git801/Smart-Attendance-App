/*
 * Copyright (c) 2026 Ankit. All rights reserved.
 */

package com.ankit.attendwise.data

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.ankit.attendwise.utils.AlarmScheduler
import com.ankit.attendwise.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

class CloudSyncManager(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val preferencesManager = PreferencesManager(context)
    private val TAG = "CloudSyncManager"
    private var listeners = mutableListOf<ListenerRegistration>()
    
    // Shared safety lock to prevent race conditions during local writes
    val syncMutex = Mutex()

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
                        syncMutex.withLock {
                            preferencesManager.saveUserName(cloudName)
                            Log.d(TAG, "Username updated from cloud: $cloudName")
                        }
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
                                val subject = dc.document.toObject(Subject::class.java).copy(id = dc.document.id)
                                val isAdded = dc.type == DocumentChange.Type.ADDED
                                
                                scope.launch { 
                                    // FETCH FIRST: Get cloud data outside the mutex to prevent locking the UI
                                    val schedules = if (isAdded) getSchedulesForSubject(subject.id) else emptyList()
                                    
                                    syncMutex.withLock {
                                        dao.upsertSubject(subject) 
                                        
                                        // RACE CONDITION FIX: If a new subject arrives, proactively fetch its schedules
                                        // from the cloud to ensure they aren't missed due to Foreign Key timing issues.
                                        if (isAdded && schedules.isNotEmpty()) {
                                            schedules.forEach { schedule ->
                                                dao.insertSchedule(schedule)
                                                AlarmScheduler.scheduleClassAlarm(context, subject, schedule)
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deserializing subject: ${e.message}")
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val id = dc.document.id
                            scope.launch { 
                                syncMutex.withLock {
                                    // CANCEL ALARMS BEFORE DELETE
                                    val schedules = dao.getSchedulesForSubject(id)
                                    schedules.forEach { AlarmScheduler.cancelClassAlarm(context, it) }
                                    dao.deleteSubjectById(id) 
                                }
                            }
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
                                val schedule = dc.document.toObject(ClassSchedule::class.java).copy(id = dc.document.id)
                                scope.launch { 
                                    syncMutex.withLock {
                                        // Ensure we don't insert a schedule if its subject was just deleted locally
                                        val subject = dao.getSubjectById(schedule.subjectId)
                                        if (subject != null) {
                                            dao.insertSchedule(schedule) 
                                            // Reschedule/Update alarm
                                            AlarmScheduler.scheduleClassAlarm(context, subject, schedule)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deserializing schedule: ${e.message}")
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val id = dc.document.id
                            scope.launch { 
                                syncMutex.withLock {
                                    val schedule = dao.getScheduleById(id)
                                    if (schedule != null) {
                                        AlarmScheduler.cancelClassAlarm(context, schedule)
                                        dao.deleteScheduleById(id) 
                                    }
                                }
                            }
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
                                val record = dc.document.toObject(AttendanceRecord::class.java).copy(id = dc.document.id)
                                scope.launch { 
                                    syncMutex.withLock {
                                        dao.insertAttendanceRecord(record) 
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deserializing record: ${e.message}")
                            }
                        }
                        DocumentChange.Type.REMOVED -> {
                            val id = dc.document.id
                            scope.launch { 
                                syncMutex.withLock {
                                    dao.deleteAttendanceRecordById(id) 
                                }
                            }
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

    suspend fun getSchedulesForSubject(subjectId: String): List<ClassSchedule> {
        val userId = getUserId() ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(userId)
                .collection("schedules")
                .whereEqualTo("subjectId", subjectId)
                .get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(ClassSchedule::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching schedules for subject: ${e.message}")
            emptyList()
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
            
            // 1. Restore Subjects with correct IDs
            val subjectsSnapshot = userDoc.collection("subjects").get().await()
            val subjects = subjectsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Subject::class.java)?.copy(id = doc.id)
            }.filter { it.id != Constants.ID_SUBJECT_HOLIDAY } // DAO handles the system subject
            Log.d(TAG, "Fetched ${subjects.size} subjects from cloud")

            // 2. Restore Schedules with correct IDs
            val schedulesSnapshot = userDoc.collection("schedules").get().await()
            val rawSchedules = schedulesSnapshot.documents.mapNotNull { doc ->
                doc.toObject(ClassSchedule::class.java)?.copy(id = doc.id)
            }
            
            // SANITIZATION: Only keep schedules that point to a subject we actually fetched
            val subjectIds = subjects.map { it.id }.toSet()
            val schedules = rawSchedules.filter { it.subjectId in subjectIds }

            // 3. Restore Attendance Records with correct IDs
            val recordsSnapshot = userDoc.collection("attendance_records").get().await()
            val rawRecords = recordsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(AttendanceRecord::class.java)?.copy(id = doc.id)
            }
            
            // SANITIZATION: Only keep records that point to a valid subject or are HOLIDAYs
            val records = rawRecords.filter { 
                it.subjectId == Constants.ID_SUBJECT_HOLIDAY || it.subjectId in subjectIds 
            }
            
            // Batch insert into local DB
            dao.restoreDataBatch(subjects, schedules, records)

            Log.d(TAG, "All data successfully restored and mapped from cloud")
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

            Log.d(TAG, "All cloud data deleted for user: $userId using batches")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cloud data: ${e.message}")
        }
    }
}
