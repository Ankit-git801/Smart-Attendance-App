/*
 * Copyright (c) 2026 Ankit. All rights reserved.
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 */

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
import com.ankit.attendwise.utils.Constants.ID_SCHEDULE_EXTRA
import com.ankit.attendwise.utils.Constants.ID_SCHEDULE_HOLIDAY
import com.ankit.attendwise.utils.Constants.ID_SCHEDULE_MANUAL
import com.ankit.attendwise.utils.Constants.ID_SCHEDULE_PAST
import com.ankit.attendwise.utils.Constants.ID_SUBJECT_HOLIDAY
import com.ankit.attendwise.utils.NotificationHelper
import com.ankit.attendwise.R
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import kotlin.math.ceil
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
    private val preferencesManager = PreferencesManager(application)
    private val cloudSyncManager = CloudSyncManager(application)
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(application)

    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubjects: StateFlow<List<Subject>> = attendanceDao.getAllSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjectsWithAttendance: StateFlow<List<SubjectWithAttendance>> =
        attendanceDao.getSubjectsWithAttendance()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchedules: StateFlow<List<ClassSchedule>> = attendanceDao.getAllSchedules()
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

    // REACIVE DATE: Updates every 30 seconds to handle midnight and "Live" class badge transitions
    val currentDate: StateFlow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now())
            kotlinx.coroutines.delay(30_000) // 30 seconds for better "Live" badge accuracy
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalDate.now())

    val isTodayHoliday: StateFlow<Boolean> = currentDate
        .flatMapLatest { date -> attendanceDao.isDateHolidayFlow(date.toEpochDay()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val todaysScheduleWithSubjects: StateFlow<List<ScheduleWithSubject>> =
        getTodaysSchedule().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _attendanceActionFeedback = MutableSharedFlow<String>()
    val attendanceActionFeedback = _attendanceActionFeedback.asSharedFlow()

    private val _showHolidayDialog = MutableStateFlow<LocalDate?>(null)

    private val _updateAvailable = MutableStateFlow(false)
    val updateAvailable: StateFlow<Boolean> = _updateAvailable.asStateFlow()

    private val _navigationEvents = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val navigationEvents = _navigationEvents.receiveAsFlow()

    private val _isForceUpdate = MutableStateFlow(false)
    val isForceUpdate: StateFlow<Boolean> = _isForceUpdate.asStateFlow()

    init {
        // Start real-time sync if user is already logged in
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
            cloudSyncManager.startRealTimeSync(attendanceDao, viewModelScope)
        }

        // SELF-HEALING: Reschedule all alarms on startup in background to ensure system consistency
        viewModelScope.launch(Dispatchers.IO) {
            rescheduleAllAlarms()
        }

        viewModelScope.launch {
            try {
                // Non-blocking check for updates
                val updateInfo = cloudSyncManager.getUpdateInfo()
                if (updateInfo.latestVersionCode > com.ankit.attendwise.BuildConfig.VERSION_CODE) {
                    _isForceUpdate.value = updateInfo.isForceUpdate
                    _updateAvailable.value = true
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Update check failed: ${e.message}")
            }
        }
    }

    val bunkAnalysisMap: StateFlow<Map<String, BunkAnalysis>> = subjectsWithAttendance
        .map { subjects ->
            subjects.associate { it.subject.id to calculateBunkAnalysisFromData(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val overallStatistics: StateFlow<AttendanceStatistics> = attendanceDao.getOverallStatisticsFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AttendanceStatistics(0, 0, 0, 0.0, 0)
        )

    private fun calculateBunkAnalysisFromData(data: SubjectWithAttendance): BunkAnalysis {
        val attended = data.presentClasses
        val total = data.totalClasses
        val target = data.subject.targetAttendance.toDouble()

        if (total == 0) return BunkAnalysis(0, 0)
        
        // BOUNDARY CHECK: Target 0%
        if (target <= 0) return BunkAnalysis(classesToBunk = 999, classesToAttend = 0)
        
        // BOUNDARY CHECK: Target 100%
        if (target >= 100.0) {
            // Fix division by zero if target is 100
            return if (attended >= total) BunkAnalysis(0, 0) 
            else BunkAnalysis(0, 999) // User-friendly 'Impossible' cap
        }

        val currentPercentage = (attended.toDouble() / total) * 100.0

        return if (currentPercentage >= target) {
            // Formula: B <= (100*A / T) - N
            val bunksAllowed = ((100.0 * attended) / target).toInt() - total
            BunkAnalysis(classesToBunk = bunksAllowed.coerceAtLeast(0), classesToAttend = 0)
        } else {
            // Formula: M >= (T*N - 100*A) / (100 - T)
            val mustAttend = ceil((target * total - 100.0 * attended) / (100.0 - target)).toInt()
            BunkAnalysis(classesToBunk = 0, classesToAttend = mustAttend.coerceAtLeast(0))
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.saveTheme(theme)
            firebaseAnalytics.logEvent("change_theme") {
                param("theme_name", theme)
            }
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

    fun triggerNavigation(subjectId: String) {
        viewModelScope.launch {
            _navigationEvents.send(subjectId)
        }
    }

    fun addOrUpdateSubject(
        subject: Subject,
        schedules: List<ClassSchedule>,
        pastPresent: Int = 0,
        pastAbsent: Int = 0
    ) {
        viewModelScope.launch {
            // ATOMIC FIX: Move the entire operation inside NonCancellable to ensure
            // that if the user saves and immediately exits, the sync completes.
            withContext(NonCancellable) {
                cloudSyncManager.syncMutex.withLock {
                    val isNewSubject = subject.id.isEmpty() || subject.id == "0"
                    val finalSubject = if (isNewSubject) subject.copy(id = UUID.randomUUID().toString()) else subject
                    val subjectId = finalSubject.id
                    
                    Log.d("AppViewModel", "Updating subject: ${finalSubject.name} ($subjectId)")
                    attendanceDao.upsertSubject(finalSubject)
                    cloudSyncManager.syncSubject(finalSubject)

                    // DEEP CLEAN FIX: Instead of relying on local cache, we query the cloud 
                    // truth to identify and wipe any orphaned "ghost" schedules.
                    if (!isNewSubject) {
                        try {
                            val cloudSchedules = cloudSyncManager.getSchedulesForSubject(subjectId)
                            val newScheduleIds = schedules.map { it.id }.filter { it.isNotBlank() }.toSet()
                            
                            val toDeleteCloud = cloudSchedules.filter { it.id !in newScheduleIds }
                            if (toDeleteCloud.isNotEmpty()) {
                                Log.d("AppViewModel", "Wiping ${toDeleteCloud.size} ghost schedules from cloud.")
                                cloudSyncManager.deleteSchedules(toDeleteCloud.map { it.id })
                            }
                            
                            // Also wipe locally to be safe
                            val localSchedules = attendanceDao.getSchedulesForSubject(subjectId)
                            localSchedules.forEach { 
                                if (it.id !in newScheduleIds) {
                                    AlarmScheduler.cancelClassAlarm(getApplication(), it)
                                    attendanceDao.deleteSchedule(it)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AppViewModel", "Deep clean failed: ${e.message}")
                        }
                    }

                    schedules.forEach { schedule ->
                        val finalScheduleId = if (schedule.id.isBlank()) UUID.randomUUID().toString() else schedule.id
                        val newSchedule = schedule.copy(id = finalScheduleId, subjectId = subjectId)

                        attendanceDao.insertSchedule(newSchedule)
                        cloudSyncManager.syncSchedule(newSchedule)
                    }

                    if (pastPresent > 0 || pastAbsent > 0) {
                        performAddPastRecords(subjectId, pastPresent, pastAbsent)
                    }

                    val currentSchedules = attendanceDao.getSchedulesForSubject(subjectId)
                    currentSchedules.forEach { schedule ->
                        AlarmScheduler.scheduleClassAlarm(getApplication(), finalSubject, schedule)
                    }
                }
            }
        }
    }

    fun addPastRecords(subjectId: String, presentCount: Int, absentCount: Int) {
        viewModelScope.launch {
            cloudSyncManager.syncMutex.withLock {
                performAddPastRecords(subjectId, presentCount, absentCount)
            }
        }
    }

    private suspend fun performAddPastRecords(subjectId: String, presentCount: Int, absentCount: Int) {
        val note = "Past Record"
        val today = LocalDate.now()
        val newRecords = mutableListOf<AttendanceRecord>()
        
        val existingRecords = attendanceDao.getAttendanceRecordsForSubject(subjectId).first()
        val existingDates = existingRecords.map { it.date }.toSet()
        
        // Fetch all holiday dates to skip them
        val allHolidays = attendanceDao.getAllAttendanceRecords().first()
            .filter { it.type == RecordType.HOLIDAY }
            .map { it.date }
            .toSet()

        var dayOffset = 1
        var presentAdded = 0
        while (presentAdded < presentCount && dayOffset < 365) {
            val date = today.minusDays(dayOffset.toLong()).toEpochDay()
            if (date !in existingDates && date !in allHolidays) {
                val record = AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    scheduleId = ID_SCHEDULE_PAST,
                    date = date,
                    isPresent = true,
                    note = note,
                    type = RecordType.MANUAL
                )
                newRecords.add(record)
                presentAdded++
            }
            dayOffset++
        }

        var absentAdded = 0
        while (absentAdded < absentCount && dayOffset < 365) {
            val date = today.minusDays(dayOffset.toLong()).toEpochDay()
            val dateAsLong = date
            if (dateAsLong !in existingDates && dateAsLong !in allHolidays && newRecords.none { it.date == dateAsLong }) {
                val record = AttendanceRecord(
                    id = UUID.randomUUID().toString(),
                    subjectId = subjectId,
                    scheduleId = ID_SCHEDULE_PAST,
                    date = dateAsLong,
                    isPresent = false,
                    note = note,
                    type = RecordType.MANUAL
                )
                newRecords.add(record)
                absentAdded++
            }
            dayOffset++
        }
        
        if (newRecords.isNotEmpty()) {
            attendanceDao.insertAttendanceRecords(newRecords)
            cloudSyncManager.syncAttendanceRecords(newRecords)
            _attendanceActionFeedback.emit(getApplication<Application>().getString(R.string.feedback_past_records_added))
            checkAndTriggerLowAttendanceWarning(subjectId)
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            firebaseAnalytics.logEvent("delete_subject") {
                param("subject_name", subject.name)
            }
            val schedules = attendanceDao.getSchedulesForSubject(subject.id)
            schedules.forEach { schedule ->
                AlarmScheduler.cancelClassAlarm(getApplication(), schedule)
            }
            
            // ATOMIC FIX: Ensure deletion is robust and not cancelled
            withContext(NonCancellable) {
                cloudSyncManager.syncMutex.withLock {
                    try {
                        attendanceDao.deleteAttendanceRecordsForSubject(subject.id)
                        attendanceDao.deleteSchedulesForSubject(subject.id)
                        attendanceDao.deleteSubject(subject)
                        
                        cloudSyncManager.deleteSubject(subject.id)
                    } catch (e: Exception) {
                        Log.e("AppViewModel", "Error deleting subject: ${e.message}")
                    }
                }
            }
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dateAsLong = date.toEpochDay()

                // 1. Check for Holiday - Don't mark attendance if it's a holiday
                val dayRecords = attendanceDao.getAllAttendanceRecordsOnDateNow(dateAsLong)
                if (dayRecords.any { it.type == RecordType.HOLIDAY }) {
                    Log.d("AppViewModel", "Skipping markAttendance: Today is a Holiday")
                    _attendanceActionFeedback.emit("Cannot mark attendance: This day is marked as a Holiday.")
                    return@launch
                }

                // 2. Wrap everything in a Sync Mutex to prevent real-time re-insertion
                cloudSyncManager.syncMutex.withLock {
                    // 2a. Identify existing records to clean up to prevent duplicates
                    val recordIdsToClean = mutableListOf<String>()
                    if (scheduleId.isNotEmpty() && scheduleId != ID_SCHEDULE_MANUAL && scheduleId != ID_SCHEDULE_PAST) {
                        val existingRecords =
                            attendanceDao.getAttendanceRecordsForSubjectOnDate(subjectId, dateAsLong)
                        // If marking a specific schedule, clean up that schedule and any general 'Manual' marks
                        recordIdsToClean.addAll(existingRecords.filter { it.scheduleId == scheduleId || it.scheduleId == ID_SCHEDULE_MANUAL }.map { it.id })
                    } else if ((type == RecordType.MANUAL || type == RecordType.CANCELLED) && scheduleId == ID_SCHEDULE_MANUAL) {
                        val existingRecords =
                            attendanceDao.getAttendanceRecordsForSubjectOnDate(subjectId, dateAsLong)
                        // If marking the whole day (Manual/Cancelled), clean up EVERYTHING for this subject today
                        recordIdsToClean.addAll(existingRecords.map { it.id })
                    }

                    // 3. Create new record
                    val record = AttendanceRecord(
                        id = UUID.randomUUID().toString(),
                        subjectId = subjectId,
                        scheduleId = scheduleId,
                        date = dateAsLong,
                        isPresent = isPresent,
                        type = type,
                        note = note
                    )

                    // 4. Perform atomic local update
                    attendanceDao.markAttendanceTransaction(recordIdsToClean, record)
                    
                    firebaseAnalytics.logEvent("mark_attendance") {
                        param("type", type.name)
                        param("is_present", if (isPresent) 1L else 0L)
                    }

                    // 5. Fire-and-forget cloud sync and notification cleanup in the background
                    launch {
                        withContext(NonCancellable) {
                            try {
                                if (recordIdsToClean.isNotEmpty()) {
                                    cloudSyncManager.deleteAttendanceRecords(recordIdsToClean)
                                }
                                cloudSyncManager.syncAttendanceRecord(record)

                                if (scheduleId.isNotEmpty() && scheduleId != ID_SCHEDULE_MANUAL) {
                                    NotificationHelper.cancelNotification(getApplication(), scheduleId.hashCode())
                                } else if (scheduleId == ID_SCHEDULE_MANUAL && date == LocalDate.now()) {
                                    // If marking manual for today, cancel any active notifications for this subject's schedules today
                                    val dayOfWeek = (date.dayOfWeek.value % 7) + 1
                                    attendanceDao.getSchedulesForDayNow(dayOfWeek).forEach { s ->
                                        if (s.subjectId == subjectId) {
                                            NotificationHelper.cancelNotification(getApplication(), s.id.hashCode())
                                        }
                                    }
                                }

                                checkAndTriggerLowAttendanceWarning(subjectId)
                            } catch (e: Exception) {
                                Log.e("AppViewModel", "Background sync error: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error marking attendance: ${e.message}")
            }
        }
    }

    fun updateAttendanceRecord(subjectId: String, date: LocalDate, isPresent: Boolean) {
        val note = if (isPresent) "Marked Present" else "Marked Absent"
        markAttendance(subjectId, ID_SCHEDULE_MANUAL, date, RecordType.MANUAL, isPresent, note)
        viewModelScope.launch {
            _attendanceActionFeedback.emit(getApplication<Application>().getString(R.string.feedback_attendance_updated))
        }
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

    fun markDateAsCancelled(subjectId: String, date: LocalDate) {
        markAttendance(subjectId, ID_SCHEDULE_MANUAL, date, RecordType.CANCELLED, false, "Class Cancelled")
        viewModelScope.launch {
            _attendanceActionFeedback.emit(getApplication<Application>().getString(R.string.feedback_attendance_updated))
        }
    }

    fun addExtraClasses(subjectId: String, date: LocalDate, isPresent: Boolean, count: Int) {
        viewModelScope.launch {
            val dateAsLong = date.toEpochDay()
            
            // Check for Holiday
            val dayRecords = attendanceDao.getAllAttendanceRecordsOnDateNow(dateAsLong)
            if (dayRecords.any { it.type == RecordType.HOLIDAY }) {
                _attendanceActionFeedback.emit(getApplication<Application>().getString(R.string.error_holiday_manual_mark))
                return@launch
            }

            withContext(NonCancellable) {
                cloudSyncManager.syncMutex.withLock {
                    val note = "Extra Class"
                    val newRecords = mutableListOf<AttendanceRecord>()
                    repeat(count) {
                        val record = AttendanceRecord(
                            id = UUID.randomUUID().toString(),
                            subjectId = subjectId,
                            scheduleId = ID_SCHEDULE_EXTRA,
                            date = dateAsLong,
                            isPresent = isPresent,
                            type = RecordType.MANUAL,
                            note = note
                        )
                        newRecords.add(record)
                    }
                    attendanceDao.insertAttendanceRecords(newRecords)
                    cloudSyncManager.syncAttendanceRecords(newRecords)
                    _attendanceActionFeedback.emit(getApplication<Application>().getString(R.string.feedback_extra_class_added))
                    checkAndTriggerLowAttendanceWarning(subjectId)
                }
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                cloudSyncManager.syncMutex.withLock {
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
                cloudSyncManager.deleteAttendanceRecord(holidayRecord.id)
                
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
        Log.d("AppViewModel", "onHolidayToggleConfirmed called.")
        viewModelScope.launch {
            _showHolidayDialog.value?.let { date ->
                withContext(NonCancellable) {
                    cloudSyncManager.syncMutex.withLock {
                        val dateAsLong = date.toEpochDay()
                        
                        // 1. Get IDs for cloud cleanup before deleting locally
                        val allDayRecords = attendanceDao.getAllAttendanceRecordsOnDateNow(dateAsLong)
                        val recordIdsToDelete = allDayRecords.map { it.id }
                        
                        // 2. Insert holiday record atomically
                        val holidayRecord = AttendanceRecord(
                            id = UUID.randomUUID().toString(),
                            subjectId = ID_SUBJECT_HOLIDAY,
                            scheduleId = ID_SCHEDULE_HOLIDAY,
                            date = dateAsLong,
                            isPresent = false,
                            note = "Holiday",
                            type = RecordType.HOLIDAY
                        )
                        
                        attendanceDao.markHolidayTransaction(dateAsLong, holidayRecord)
                        
                        // 3. Sync deletions and insertion to cloud
                        cloudSyncManager.deleteAttendanceRecords(recordIdsToDelete)
                        cloudSyncManager.syncAttendanceRecord(holidayRecord)

                        // 4. Cancel TODAY'S alarms but immediately reschedule for NEXT WEEK
                        // This ensures the chain of weekly reminders isn't broken by a single holiday.
                        val calendarDayOfWeek = (date.dayOfWeek.value % 7) + 1
                        val schedulesForDay = attendanceDao.getSchedulesForDayNow(calendarDayOfWeek)
                        val allSubjectsList = attendanceDao.getAllSubjects().first()
                        
                        schedulesForDay.forEach { schedule ->
                            // Cancel today
                            AlarmScheduler.cancelClassAlarm(getApplication(), schedule)
                            NotificationHelper.cancelNotification(getApplication(), schedule.id.hashCode())
                            
                            // Reschedule for next week (leap-frog)
                            val subject = allSubjectsList.find { it.id == schedule.subjectId }
                            if (subject != null) {
                                AlarmScheduler.scheduleClassAlarm(getApplication(), subject, schedule, forceNextWeek = true)
                            }
                        }
                    }
                }
            }
            _showHolidayDialog.value = null
        }
    }


    private fun getTodaysSchedule(): Flow<List<ScheduleWithSubject>> {
        // TIMER: A flow that emits every 30 seconds to force re-evaluation of "Live" class status and sorting
        val refreshTimer = flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(30_000)
            }
        }

        return currentDate.flatMapLatest { todayDate ->
            val todayDayOfWeek = (todayDate.dayOfWeek.value % 7) + 1 // Convert to Calendar day constant
            val todayEpochDay = todayDate.toEpochDay()
            
            combine(
                attendanceDao.getSchedulesForDay(todayDayOfWeek),
                allSubjects,
                attendanceDao.isDateHolidayFlow(todayEpochDay),
                allAttendanceRecords,
                refreshTimer
            ) { schedules, subjects, isTodayHoliday, records, _ ->
                if (isTodayHoliday) {
                    emptyList()
                } else {
                    val now = java.time.LocalTime.now()
                    
                    val regularClasses = schedules.mapNotNull { schedule ->
                        val subject = subjects.find { it.id == schedule.subjectId } ?: return@mapNotNull null
                        val record = records.find {
                            val sId = schedule.id
                            (it.scheduleId == sId || it.scheduleId == ID_SCHEDULE_MANUAL) &&
                                    it.date == todayEpochDay &&
                                    it.subjectId == subject.id
                        }
                        
                        val start = java.time.LocalTime.of(schedule.startHour, schedule.startMinute)
                        val end = java.time.LocalTime.of(schedule.endHour, schedule.endMinute)
                        val isLive = (now == start || now.isAfter(start)) && now.isBefore(end)
                        val isCompleted = now.isAfter(end)
                        
                        ScheduleWithSubject(schedule, subject, record, isLive, isCompleted)
                    }

                    val extraClasses = records.filter { 
                        it.date == todayEpochDay && it.scheduleId == ID_SCHEDULE_EXTRA 
                    }.mapNotNull { record ->
                        val subject = subjects.find { it.id == record.subjectId } ?: return@mapNotNull null
                        // Synthetic schedule for UI representation
                        val syntheticSchedule = ClassSchedule(
                            id = record.id,
                            subjectId = subject.id,
                            dayOfWeek = todayDayOfWeek,
                            startHour = 23, // Sort to bottom
                            startMinute = 59
                        )
                        ScheduleWithSubject(syntheticSchedule, subject, record, isLive = false, isCompleted = true)
                    }

                    (regularClasses + extraClasses).sortedWith(
                        compareByDescending<ScheduleWithSubject> { it.isLive }
                            .thenBy { it.schedule.startHour }
                            .thenBy { it.schedule.startMinute }
                    )
                }
            }
        }
    }

    fun getWeeklySchedule(): Flow<Map<Int, List<ScheduleWithSubject>>> {
        return attendanceDao.getAllSchedules().combine(allSubjects) { allSchedules, allSubjects ->
            allSchedules.groupBy { it.dayOfWeek ?: 0 }
                .mapValues { entry ->
                    entry.value.mapNotNull { schedule ->
                        allSubjects.find { it.id == schedule.subjectId }?.let { subject ->
                            ScheduleWithSubject(schedule, subject, isLive = false, isCompleted = false)
                        }
                    }.sortedBy { it.schedule.startHour ?: 0 }
            }
        }
    }

    fun restoreDataFromCloud(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val success = cloudSyncManager.restoreAllData(attendanceDao)
                if (success) {
                    rescheduleAllAlarms()
                }
                onComplete(success)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun rescheduleAllAlarms() {
        viewModelScope.launch {
            try {
                val subjects = attendanceDao.getAllSubjects().first()
                subjects.forEach { subject ->
                    val schedules = attendanceDao.getSchedulesForSubject(subject.id)
                    schedules.forEach { schedule ->
                        AlarmScheduler.scheduleClassAlarm(getApplication(), subject, schedule)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppViewModel", "Error rescheduling alarms: ${e.message}")
            }
        }
    }

    fun signUpWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // SYNC FIX: Upload current local state to the new account
                            viewModelScope.launch {
                                uploadAllLocalDataToCloud()
                                cloudSyncManager.startRealTimeSync(attendanceDao, viewModelScope)
                            }
                        }
                        onComplete(task.isSuccessful, task.exception?.message)
                    }
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    private suspend fun uploadAllLocalDataToCloud() {
        val subjects = allSubjects.first()
        val schedules = attendanceDao.getAllSchedules().first()
        val records = allAttendanceRecords.first()

        // 1. Sync Profile Name
        cloudSyncManager.syncUserProfile(userName.value)
        
        // 2. Sync Subjects (Sequential to ensure parent exists)
        subjects.forEach { cloudSyncManager.syncSubject(it) }
        
        // 3. Sync Schedules and Records in batches
        cloudSyncManager.syncSchedules(schedules)
        cloudSyncManager.syncAttendanceRecords(records)
        
        Log.d("AppViewModel", "Offline data migration complete: ${subjects.size} subjects synced.")
    }

    fun loginWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                auth.signInWithEmailAndPassword(email, password).await()
                
                // 1. Restore the original name from cloud
                Log.d("AppViewModel", "Fetching stored name from cloud...")
                val storedName = cloudSyncManager.getUserProfileName()
                Log.d("AppViewModel", "Cloud name result: '$storedName'")
                
                if (!storedName.isNullOrBlank()) {
                    preferencesManager.saveUserName(storedName)
                    Log.d("AppViewModel", "Restored name from cloud: $storedName")
                } else {
                    Log.w("AppViewModel", "No name found in cloud for this user.")
                }
                
                // 2. Restore all attendance data silently
                Log.d("AppViewModel", "Starting full data restore...")
                // Cancel existing alarms before restore to avoid duplicates/orphans
                val existingSubjects = attendanceDao.getAllSubjects().first()
                existingSubjects.forEach { s ->
                    attendanceDao.getSchedulesForSubject(s.id).forEach { 
                        AlarmScheduler.cancelClassAlarm(getApplication(), it)
                    }
                }
                
                val success = cloudSyncManager.restoreAllData(attendanceDao)
                if (success) {
                    cloudSyncManager.startRealTimeSync(attendanceDao, viewModelScope)
                    rescheduleAllAlarms()
                    onComplete(true, null)
                } else {
                    onComplete(false, "Restore failed. Please check your internet and try again.")
                }
            } catch (e: Exception) {
                val errorMsg = e.message
                Log.e("AppViewModel", "Login failed: $errorMsg")
                onComplete(false, errorMsg)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            cloudSyncManager.stopRealTimeSync()
            // 1. Cancel all active alarms before clearing data
            val subjects = attendanceDao.getAllSubjects().first()
            subjects.forEach { subject ->
                val schedules = attendanceDao.getSchedulesForSubject(subject.id)
                schedules.forEach { schedule ->
                    AlarmScheduler.cancelClassAlarm(getApplication(), schedule)
                }
            }
            
            // 2. Clear local data
            attendanceDao.deleteAllSubjects()
            attendanceDao.deleteAllSchedules()
            attendanceDao.deleteAllAttendanceRecords()
            
            // 3. Clear preferences (Keep username for a moment to prevent 'Student' flicker during transition)
            preferencesManager.setOnboardingComplete(false)

            // 4. Clear internal ViewModel state
            _showHolidayDialog.value = null
            
            // 5. Sign out from Firebase
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            
            // Final step: Clear the name after sign out is initiated
            preferencesManager.saveUserName("")
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
