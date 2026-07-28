package com.ankit.attendwise.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankit.attendwise.data.*
import com.ankit.attendwise.models.AttendanceRecordWithSubject
import com.ankit.attendwise.models.AttendanceStatistics
import com.ankit.attendwise.models.ScheduleWithSubject
import com.ankit.attendwise.models.SubjectWithAttendance
import com.ankit.attendwise.utils.AlarmScheduler
import com.ankit.attendwise.utils.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.util.*

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
    private val preferencesManager = PreferencesManager(application)
    private val cloudSyncManager = CloudSyncManager(application)

    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubjects: StateFlow<List<Subject>> = attendanceDao.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectsWithAttendance: StateFlow<List<SubjectWithAttendance>> =
        attendanceDao.getSubjectsWithAttendance()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val theme = preferencesManager.themeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "System Default"
    )
    val userName = preferencesManager.userNameFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val isOnboardingComplete: StateFlow<Boolean?> = preferencesManager.isOnboardingCompleteFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    val todaysScheduleWithSubjects: StateFlow<List<ScheduleWithSubject>> =
        getTodaysSchedule().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _showHolidayDialog = MutableStateFlow<LocalDate?>(null)
    val showHolidayDialog: StateFlow<LocalDate?> = _showHolidayDialog.asStateFlow()

    private val _processingScheduleIds = MutableStateFlow<Set<String>>(emptySet())
    val processingScheduleIds: StateFlow<Set<String>> = _processingScheduleIds.asStateFlow()

    val bunkAnalysisMap: StateFlow<Map<String, BunkAnalysis>> = subjectsWithAttendance
        .map { subjects ->
            subjects.associate { it.subject.id to calculateBunkAnalysisFromData(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private fun calculateBunkAnalysisFromData(data: SubjectWithAttendance): BunkAnalysis {
        val attended = data.presentClasses
        val total = data.totalClasses
        val target = data.subject.targetAttendance.toDouble()

        if (total == 0) return BunkAnalysis(0, 0)
        if (target <= 0) return BunkAnalysis(classesToBunk = 999, classesToAttend = 0) // Safety check

        val currentPercentage = (attended.toDouble() / total) * 100.0

        return if (currentPercentage >= target) {
            var bunksAllowed = 0
            while (true) {
                val futureTotal = total + 1 + bunksAllowed
                val futurePercentage = (attended.toDouble() / futureTotal) * 100
                if (futurePercentage < target || bunksAllowed > 500) { // Safety break
                    break
                }
                bunksAllowed++
            }
            BunkAnalysis(classesToBunk = bunksAllowed, classesToAttend = 0)
        } else {
            var mustAttend = 0
            while (true) {
                val futureTotal = total + mustAttend
                if (futureTotal == 0) {
                    mustAttend++
                    continue
                }
                val futurePercentage = ((attended + mustAttend).toDouble() / futureTotal) * 100
                if (futurePercentage >= target) {
                    break
                }
                mustAttend++
                if (mustAttend > 1000) return BunkAnalysis(0, Int.MAX_VALUE)
            }
            BunkAnalysis(classesToBunk = 0, classesToAttend = mustAttend)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.saveTheme(theme)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.saveUserName(name)
            cloudSyncManager.syncUserProfile(name)
        }
    }

    fun completeOnboarding(name: String) {
        viewModelScope.launch {
            preferencesManager.saveUserName(name)
            preferencesManager.setOnboardingComplete(true)
            cloudSyncManager.syncUserProfile(name)
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingComplete(true)
        }
    }

    fun addOrUpdateSubject(
        subject: Subject,
        schedules: List<ClassSchedule>,
        pastPresent: Int = 0,
        pastAbsent: Int = 0
    ) {
        viewModelScope.launch {
            val isNewSubject = subject.id.isEmpty() || subject.id == "0" // Safety check for old IDs
            val finalSubject = if (isNewSubject) subject.copy(id = UUID.randomUUID().toString()) else subject
            val subjectId = finalSubject.id
            attendanceDao.insertSubject(finalSubject)
            cloudSyncManager.syncSubject(finalSubject)

            if (!isNewSubject) {
                val oldSchedules = attendanceDao.getSchedulesForSubject(subjectId)
                val newScheduleIds = schedules.map { it.id }.filter { it.isNotBlank() }.toSet()

                // Only delete schedules that are no longer in the list
                oldSchedules.forEach { oldSchedule ->
                    if (oldSchedule.id !in newScheduleIds) {
                        AlarmScheduler.cancelClassAlarm(getApplication(), oldSchedule)
                        attendanceDao.deleteSchedule(oldSchedule)
                        cloudSyncManager.deleteSchedule(oldSchedule.id)
                    }
                }
            }

            schedules.forEach { schedule ->
                // Preserve ID if it exists, otherwise generate new one
                val finalScheduleId = if (schedule.id.isBlank()) UUID.randomUUID().toString() else schedule.id
                val newSchedule = schedule.copy(id = finalScheduleId, subjectId = subjectId)

                attendanceDao.insertSchedule(newSchedule)
                cloudSyncManager.syncSchedule(newSchedule)
            }

            // Handle Past Attendance if provided
            if (pastPresent > 0 || pastAbsent > 0) {
                addPastRecords(subjectId, pastPresent, pastAbsent)
            }

            // Re-schedule all current alarms to pick up potential subject changes (name/color)
            val currentSchedules = attendanceDao.getSchedulesForSubject(subjectId)
            currentSchedules.forEach { schedule ->
                AlarmScheduler.scheduleClassAlarm(
                    getApplication(),
                    finalSubject,
                    schedule
                )
            }
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            val schedules = attendanceDao.getSchedulesForSubject(subject.id)
            schedules.forEach { schedule ->
                AlarmScheduler.cancelClassAlarm(getApplication(), schedule)
                cloudSyncManager.deleteSchedule(schedule.id)
            }
            attendanceDao.deleteAttendanceRecordsForSubject(subject.id)
            attendanceDao.deleteSchedulesForSubject(subject.id)
            attendanceDao.deleteSubject(subject)
            cloudSyncManager.deleteSubject(subject.id)
        }
    }

    private fun markAttendance(
        subjectId: String,
        scheduleId: String,
        date: LocalDate,
        type: RecordType,
        isPresent: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            if (scheduleId.isNotEmpty()) {
                _processingScheduleIds.update { it + scheduleId }
            }
            try {
                val dateAsLong = date.toEpochDay()

                // 1. Check for Holiday - Don't mark attendance if it's a holiday
                val dayRecords = attendanceDao.getAllAttendanceRecordsOnDateNow(dateAsLong)
                if (dayRecords.any { it.type == RecordType.HOLIDAY }) {
                    Log.d("AppViewModel", "Skipping markAttendance: Today is a Holiday")
                    return@launch
                }

                // 2. Clean up existing records for this specific slot to prevent duplicates
                // We do this manually to ensure Cloud Sync is notified of deletions
                if (scheduleId.isNotEmpty() && scheduleId != "-1" && scheduleId != "-3") {
                    val existingRecords =
                        attendanceDao.getAttendanceRecordsForSubjectOnDate(subjectId, dateAsLong)
                    existingRecords.filter { it.scheduleId == scheduleId || it.scheduleId == "-1" }
                        .forEach { record ->
                            attendanceDao.deleteAttendanceRecord(record)
                            cloudSyncManager.deleteAttendanceRecord(record.id)
                        }
                } else if (type == RecordType.MANUAL && scheduleId == "-1") {
                    val existingRecords =
                        attendanceDao.getAttendanceRecordsForSubjectOnDate(subjectId, dateAsLong)
                    existingRecords.forEach { record ->
                        attendanceDao.deleteAttendanceRecord(record)
                        cloudSyncManager.deleteAttendanceRecord(record.id)
                    }
                }

                // 3. Insert new record
                val record = AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    scheduleId = scheduleId,
                    date = dateAsLong,
                    isPresent = isPresent,
                    type = type,
                    note = note
                )
                attendanceDao.insertAttendanceRecord(record)
                cloudSyncManager.syncAttendanceRecord(record)
                checkAndTriggerLowAttendanceWarning(subjectId)
            } finally {
                if (scheduleId.isNotEmpty()) {
                    _processingScheduleIds.update { it - scheduleId }
                }
            }
        }
    }

    fun updateAttendanceRecord(subjectId: String, date: LocalDate, isPresent: Boolean) {
        val note = if (isPresent) "Marked Present" else "Marked Absent"
        markAttendance(subjectId, "-1", date, RecordType.MANUAL, isPresent, note)
    }

    fun deleteAttendanceRecordById(recordId: String, subjectId: String) {
        viewModelScope.launch {
            val recordToDelete = attendanceDao.getAttendanceRecordById(recordId)
            if (recordToDelete != null) {
                attendanceDao.deleteAttendanceRecord(recordToDelete)
                cloudSyncManager.deleteAttendanceRecord(recordToDelete.id)
                checkAndTriggerLowAttendanceWarning(subjectId)
            }
        }
    }

    fun deleteAttendanceRecordForDate(subjectId: String, date: LocalDate) {
        viewModelScope.launch {
            val recordsToDelete = attendanceDao.getAttendanceRecordsForSubjectOnDate(subjectId, date.toEpochDay())
            attendanceDao.deleteAttendanceRecordsForSubjectOnDate(subjectId, date.toEpochDay())
            recordsToDelete.forEach { cloudSyncManager.deleteAttendanceRecord(it.id) }
            checkAndTriggerLowAttendanceWarning(subjectId)
        }
    }

    fun markTodayAsPresent(subjectId: String, scheduleId: String) {
        markAttendance(subjectId, scheduleId, LocalDate.now(), RecordType.CLASS, true, "Marked from Home")
    }

    fun markTodayAsAbsent(subjectId: String, scheduleId: String) {
        markAttendance(subjectId, scheduleId, LocalDate.now(), RecordType.CLASS, false, "Marked from Home")
    }

    fun markTodayAsCancelled(subjectId: String, scheduleId: String) {
        markAttendance(subjectId, scheduleId, LocalDate.now(), RecordType.CANCELLED, false, "Class Cancelled")
    }

    fun addExtraClasses(subjectId: String, date: LocalDate, isPresent: Boolean, count: Int) {
        viewModelScope.launch {
            val note = "Extra Class"
            repeat(count) {
                val record = AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    scheduleId = "0",
                    date = date.toEpochDay(),
                    isPresent = isPresent,
                    type = RecordType.MANUAL,
                    note = note
                )
                attendanceDao.insertAttendanceRecord(record)
                cloudSyncManager.syncAttendanceRecord(record)
            }
            checkAndTriggerLowAttendanceWarning(subjectId)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val subjects = allSubjects.first()
            subjects.forEach { subject ->
                val schedules = attendanceDao.getSchedulesForSubject(subject.id)
                schedules.forEach { schedule ->
                    AlarmScheduler.cancelClassAlarm(getApplication(), schedule)
                }
            }
            attendanceDao.deleteAllAttendanceRecords()
            attendanceDao.deleteAllSchedules()
            attendanceDao.deleteAllSubjects()
            cloudSyncManager.deleteAllCloudData()
        }
    }

    suspend fun getSubjectById(subjectId: String): Subject? = attendanceDao.getSubjectById(subjectId)

    suspend fun getSchedulesForSubject(subjectId: String): List<ClassSchedule> =
        attendanceDao.getSchedulesForSubject(subjectId)

    fun getAttendanceRecordsForSubject(subjectId: String): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceRecordsForSubject(subjectId)
    }

    fun getRecordsForDate(date: LocalDate): Flow<List<AttendanceRecordWithSubject>> {
        return attendanceDao.getRecordsForDateWithSubject(date.toEpochDay())
    }

    private suspend fun checkAndTriggerLowAttendanceWarning(subjectId: String) {
        val subject = attendanceDao.getSubjectById(subjectId)
        if (subject != null) {
            val total = attendanceDao.getTotalClassesForSubject(subjectId)
            val present = attendanceDao.getPresentClassesForSubject(subjectId)
            val newPercentage = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0

            if (newPercentage < subject.targetAttendance && total > 0) {
                NotificationHelper.showAttendanceWarningNotification(
                    getApplication(),
                    subject,
                    newPercentage
                )
            }
        }
    }

    fun onHolidayToggleRequested(date: LocalDate) {
        viewModelScope.launch {
            val allRecords = attendanceDao.getAllAttendanceRecords().first()
            val holidayRecord = allRecords.find { it.date == date.toEpochDay() && it.type == RecordType.HOLIDAY }

            if (holidayRecord != null) {
                attendanceDao.deleteAttendanceRecord(holidayRecord)
                // Reschedule alarms for this day
                // FIX: LocalDate.dayOfWeek.value is 1 (Mon) - 7 (Sun), while Calendar uses 2 (Mon) - 1 (Sun)
                val calendarDayOfWeek = (date.dayOfWeek.value % 7) + 1
                val schedulesForDay = attendanceDao.getSchedulesForDayNow(calendarDayOfWeek)
                val allSubjectsList = attendanceDao.getAllSubjects().first()
                schedulesForDay.forEach { schedule ->
                    val subject = allSubjectsList.find { it.id == schedule.subjectId }
                    if (subject != null) {
                        AlarmScheduler.scheduleClassAlarm(getApplication(), subject, schedule)
                    }
                }
            } else {
                _showHolidayDialog.value = date
            }
        }
    }

    fun onHolidayToggleConfirmed() {
        viewModelScope.launch {
            _showHolidayDialog.value?.let { date ->
                val dateAsLong = date.toEpochDay()
                
                // 1. Find and delete ALL existing records on this date from local and cloud
                val allDayRecords = attendanceDao.getAllAttendanceRecordsOnDateNow(dateAsLong)
                allDayRecords.forEach { record ->
                    attendanceDao.deleteAttendanceRecord(record)
                    cloudSyncManager.deleteAttendanceRecord(record.id)
                }

                // 2. Insert holiday record
                val holidayRecord = AttendanceRecord(
                    id = java.util.UUID.randomUUID().toString(),
                    subjectId = "0",
                    scheduleId = "-2",
                    date = dateAsLong,
                    isPresent = false,
                    note = "Holiday",
                    type = RecordType.HOLIDAY
                )
                attendanceDao.insertAttendanceRecord(holidayRecord)
                cloudSyncManager.syncAttendanceRecord(holidayRecord)

                // 3. Cancel alarms for this day
                // FIX: LocalDate.dayOfWeek.value is 1 (Mon) - 7 (Sun), while Calendar uses 2 (Mon) - 1 (Sun)
                val calendarDayOfWeek = (date.dayOfWeek.value % 7) + 1
                val schedulesForDay = attendanceDao.getSchedulesForDayNow(calendarDayOfWeek)
                schedulesForDay.forEach { schedule ->
                    AlarmScheduler.cancelClassAlarm(getApplication(), schedule)
                }
            }
            _showHolidayDialog.value = null
        }
    }

    fun onHolidayToggleDismissed() {
        _showHolidayDialog.value = null
    }

    fun addPastRecords(subjectId: String, presentCount: Int, absentCount: Int) {
        viewModelScope.launch {
            val note = "Past Record"
            val today = LocalDate.now()

            repeat(presentCount) { i ->
                val date = today.minusDays(i.toLong() + 1).toEpochDay()
                val record = AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    scheduleId = "-3",
                    date = date,
                    isPresent = true,
                    note = note,
                    type = RecordType.MANUAL
                )
                attendanceDao.insertAttendanceRecord(record)
                cloudSyncManager.syncAttendanceRecord(record)
            }

            repeat(absentCount) { i ->
                val date = today.minusDays((presentCount + i).toLong() + 1).toEpochDay()
                val record = AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    scheduleId = "-3",
                    date = date,
                    isPresent = false,
                    note = note,
                    type = RecordType.MANUAL
                )
                attendanceDao.insertAttendanceRecord(record)
                cloudSyncManager.syncAttendanceRecord(record)
            }
            checkAndTriggerLowAttendanceWarning(subjectId)
        }
    }

    suspend fun getOverallStatistics(): AttendanceStatistics {
        val total = attendanceDao.getTotalClassesOverall()
        val present = attendanceDao.getTotalPresentOverall()
        val subjects = attendanceDao.getSubjectCount()
        return AttendanceStatistics(
            total,
            present,
            total - present,
            if (total > 0) (present.toDouble() / total) * 100 else 0.0,
            subjects
        )
    }

    suspend fun calculateBunkAnalysis(subjectId: String): BunkAnalysis {
        val subject = getSubjectById(subjectId) ?: return BunkAnalysis(0, 0)
        val attended = attendanceDao.getPresentClassesForSubject(subjectId)
        val total = attendanceDao.getTotalClassesForSubject(subjectId)
        val target = subject.targetAttendance.toDouble()

        if (total == 0) return BunkAnalysis(0, 0)
        if (target <= 0) return BunkAnalysis(classesToBunk = 999, classesToAttend = 0) // Safety check

        val currentPercentage = (attended.toDouble() / total) * 100.0

        return if (currentPercentage >= target) {
            var bunksAllowed = 0
            while (true) {
                val futureTotal = total + 1 + bunksAllowed
                val futurePercentage = (attended.toDouble() / futureTotal) * 100
                if (futurePercentage < target || bunksAllowed > 500) { // Safety break
                    break
                }
                bunksAllowed++
            }
            BunkAnalysis(classesToBunk = bunksAllowed, classesToAttend = 0)
        } else {
            var mustAttend = 0
            while (true) {
                val futureTotal = total + mustAttend
                if (futureTotal == 0) {
                    mustAttend++
                    continue
                }
                val futurePercentage = ((attended + mustAttend).toDouble() / futureTotal) * 100
                if (futurePercentage >= target) {
                    break
                }
                mustAttend++
                if (mustAttend > 1000) return BunkAnalysis(0, Int.MAX_VALUE)
            }
            BunkAnalysis(classesToBunk = 0, classesToAttend = mustAttend)
        }
    }


    private fun getTodaysSchedule(): Flow<List<ScheduleWithSubject>> {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayEpochDay = LocalDate.now().toEpochDay()
        return combine(
            attendanceDao.getSchedulesForDay(today),
            allSubjects,
            allAttendanceRecords
        ) { schedules, subjects, records ->
            val isTodayHoliday =
                records.any { it.date == todayEpochDay && it.type == RecordType.HOLIDAY }
            if (isTodayHoliday) {
                emptyList()
            } else {
                schedules.mapNotNull { schedule ->
                    val subject = subjects.find { it.id == schedule.subjectId } ?: return@mapNotNull null
                    val record = records.find {
                        (it.scheduleId == schedule.id || it.scheduleId == "-1") &&
                                it.date == todayEpochDay &&
                                it.subjectId == subject.id
                    }
                    ScheduleWithSubject(schedule, subject, record)
                }.sortedBy { it.schedule.startHour }
            }
        }
    }

    fun getWeeklySchedule(): Flow<Map<Int, List<ScheduleWithSubject>>> {
        return attendanceDao.getAllSchedules().combine(allSubjects) { allSchedules, allSubjects ->
            allSchedules.groupBy { it.dayOfWeek }
                .mapValues { entry ->
                    entry.value.mapNotNull { schedule ->
                        allSubjects.find { it.id == schedule.subjectId }?.let { subject ->
                            ScheduleWithSubject(schedule, subject)
                        }
                    }.sortedBy { it.schedule.startHour }
                }
        }
    }

    fun restoreDataFromCloud(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = cloudSyncManager.restoreAllData(attendanceDao)
            if (success) {
                rescheduleAllAlarms()
            }
            onComplete(success)
        }
    }

    private fun rescheduleAllAlarms() {
        viewModelScope.launch {
            val subjects = attendanceDao.getAllSubjects().first()
            subjects.forEach { subject ->
                val schedules = attendanceDao.getSchedulesForSubject(subject.id)
                schedules.forEach { schedule ->
                    AlarmScheduler.scheduleClassAlarm(getApplication(), subject, schedule)
                }
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        onComplete(task.isSuccessful, task.exception?.message)
                    }
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    fun loginWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email, password).await()
                
                // 1. Restore the original name from cloud
                val storedName = cloudSyncManager.getUserProfileName()
                if (!storedName.isNullOrBlank()) {
                    preferencesManager.saveUserName(storedName)
                }
                
                // 2. Clear local data before restore to prevent duplicates
                attendanceDao.deleteAllSubjects()
                attendanceDao.deleteAllSchedules()
                attendanceDao.deleteAllAttendanceRecords()

                // 3. Restore all attendance data silently
                val success = cloudSyncManager.restoreAllData(attendanceDao)
                if (success) {
                    rescheduleAllAlarms()
                }
                onComplete(true, null)
            } catch (e: Exception) {
                val errorMsg = e.message
                Log.e("AppViewModel", "Login failed: $errorMsg")
                onComplete(false, errorMsg)
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // 1. Clear local data
            attendanceDao.deleteAllSubjects()
            attendanceDao.deleteAllSchedules()
            attendanceDao.deleteAllAttendanceRecords()
            
            // 2. Clear preferences
            preferencesManager.saveUserName("")
            preferencesManager.setOnboardingComplete(false)
            
            // 3. Sign out from Firebase
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            onComplete()
        }
    }

    fun resetPassword(email: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        onComplete(task.isSuccessful, task.exception?.message)
                    }
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }
}
