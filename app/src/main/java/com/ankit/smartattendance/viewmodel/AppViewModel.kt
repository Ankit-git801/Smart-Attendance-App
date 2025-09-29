package com.ankit.smartattendance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankit.smartattendance.data.*
import com.ankit.smartattendance.models.AttendanceRecordWithSubject
import com.ankit.smartattendance.models.AttendanceStatistics
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.models.SubjectWithAttendance
import com.ankit.smartattendance.utils.AlarmScheduler
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
    private val preferencesManager = PreferencesManager(application)
    private val applicationContext = application.applicationContext

    val allSubjects: Flow<List<Subject>> = attendanceDao.getAllSubjects()
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceRecords()

    val subjectsWithAttendance: StateFlow<List<SubjectWithAttendance>> = attendanceDao.getSubjectsWithAttendance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val theme: StateFlow<String> = preferencesManager.themeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "System Default"
    )

    val userName: StateFlow<String> = preferencesManager.userNameFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "User"
    )

    val todaysScheduleWithSubjects: StateFlow<List<ScheduleWithSubject>> =
        getTodaysSchedule().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _showHolidayDialog = MutableStateFlow<LocalDate?>(null)
    val showHolidayDialog: StateFlow<LocalDate?> = _showHolidayDialog.asStateFlow()

    private val _bunkAnalysisMap = MutableStateFlow<Map<Long, BunkAnalysis>>(emptyMap())
    val bunkAnalysisMap: StateFlow<Map<Long, BunkAnalysis>> = _bunkAnalysisMap.asStateFlow()

    init {
        // Recalculate bunk analysis whenever subjects or attendance changes
        viewModelScope.launch {
            subjectsWithAttendance.collect { subjects ->
                val analysisMap = mutableMapOf<Long, BunkAnalysis>()
                subjects.forEach { subjectWithAttendance ->
                    analysisMap[subjectWithAttendance.subject.id] = calculateBunkAnalysis(subjectWithAttendance.subject.id)
                }
                _bunkAnalysisMap.value = analysisMap
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { preferencesManager.saveTheme(theme) }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.saveUserName(name)
        }
    }

    fun addOrUpdateSubject(subject: Subject, schedules: List<ClassSchedule>) {
        viewModelScope.launch {
            val isNewSubject = subject.id == 0L
            val subjectId = attendanceDao.insertSubject(subject)

            if (!isNewSubject) {
                val oldSchedules = attendanceDao.getSchedulesForSubject(subject.id)
                // DEFINITIVE FIX: Loop to cancel each specific alarm.
                oldSchedules.forEach { schedule ->
                    AlarmScheduler.cancelClassAlarm(applicationContext, schedule)
                }
                attendanceDao.deleteSchedulesForSubject(subject.id)
            }

            schedules.forEach { attendanceDao.insertSchedule(it.copy(subjectId = subjectId)) }
            val newSchedules = attendanceDao.getSchedulesForSubject(subjectId)

            // DEFINITIVE FIX: Loop to set each specific, exact alarm.
            newSchedules.forEach { schedule ->
                AlarmScheduler.scheduleClassAlarm(
                    applicationContext,
                    subject.copy(id = subjectId),
                    schedule
                )
            }
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            val schedules = attendanceDao.getSchedulesForSubject(subject.id)
            // DEFINITIVE FIX: Loop to cancel each specific alarm.
            schedules.forEach { schedule ->
                AlarmScheduler.cancelClassAlarm(applicationContext, schedule)
            }
            attendanceDao.deleteAttendanceRecordsForSubject(subject.id)
            attendanceDao.deleteSchedulesForSubject(subject.id)
            attendanceDao.deleteSubject(subject)
        }
    }

    private fun markAttendance(
        subjectId: Long,
        scheduleId: Long,
        date: LocalDate,
        type: RecordType,
        isPresent: Boolean,
        note: String,
        count: Int = 1
    ) {
        viewModelScope.launch {
            for (i in 1..count) {
                val uniqueDate = if (count > 1) System.currentTimeMillis() + i else date.toEpochDay()
                val record = AttendanceRecord(
                    subjectId = subjectId,
                    scheduleId = scheduleId,
                    date = uniqueDate,
                    isPresent = isPresent,
                    type = type,
                    note = note
                )
                attendanceDao.insertAttendanceRecord(record)
            }
            checkAndTriggerLowAttendanceWarning(subjectId)
        }
    }

    fun updateAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            attendanceDao.insertAttendanceRecord(record)
            checkAndTriggerLowAttendanceWarning(record.subjectId)
        }
    }

    fun deleteAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            attendanceDao.deleteAttendanceRecord(record)
            checkAndTriggerLowAttendanceWarning(record.subjectId)
        }
    }

    fun markTodayAsPresent(subjectId: Long, scheduleId: Long) {
        markAttendance(subjectId, scheduleId, LocalDate.now(), RecordType.CLASS, true, "Marked from Home")
    }

    fun markTodayAsAbsent(subjectId: Long, scheduleId: Long) {
        markAttendance(subjectId, scheduleId, LocalDate.now(), RecordType.CLASS, false, "Marked from Home")
    }

    fun markTodayAsCancelled(subjectId: Long, scheduleId: Long) {
        markAttendance(subjectId, scheduleId, LocalDate.now(), RecordType.CANCELLED, false, "Class Cancelled")
    }

    fun addExtraClasses(subjectId: Long, date: LocalDate, isPresent: Boolean, count: Int) {
        val note = "Extra Class (${if (isPresent) "Present" else "Absent"})"
        markAttendance(subjectId, 0L, date, RecordType.CLASS, isPresent, note, count)
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val subjects = allSubjects.first()
            subjects.forEach { subject ->
                val schedules = attendanceDao.getSchedulesForSubject(subject.id)
                schedules.forEach { schedule ->
                    AlarmScheduler.cancelClassAlarm(applicationContext, schedule)
                }
            }
            attendanceDao.deleteAllAttendanceRecords()
            attendanceDao.deleteAllSchedules()
            attendanceDao.deleteAllSubjects()
        }
    }

    suspend fun getSubjectById(subjectId: Long): Subject? = attendanceDao.getSubjectById(subjectId)

    suspend fun getSchedulesForSubject(subjectId: Long): List<ClassSchedule> = attendanceDao.getSchedulesForSubject(subjectId)

    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceRecordsForSubject(subjectId)
    }

    fun getRecordsForDate(date: LocalDate): Flow<List<AttendanceRecordWithSubject>> {
        return attendanceDao.getRecordsForDateWithSubject(date.toEpochDay())
    }

    suspend fun hasAttendanceBeenMarked(scheduleId: Long, date: LocalDate): Boolean {
        return attendanceDao.getRecordByScheduleIdAndDate(scheduleId, date.toEpochDay()) != null
    }

    private suspend fun checkAndTriggerLowAttendanceWarning(subjectId: Long) {
        val subject = attendanceDao.getSubjectById(subjectId)
        if (subject != null) {
            val total = attendanceDao.getTotalClassesForSubject(subjectId)
            val present = attendanceDao.getPresentClassesForSubject(subjectId)
            val newPercentage = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0

            if (newPercentage < subject.targetAttendance && total > 0) {
                NotificationHelper.showAttendanceWarningNotification(applicationContext, subject, newPercentage)
            }
        }
    }

    fun onHolidayToggleRequested(date: LocalDate) {
        viewModelScope.launch {
            val isAlreadyHoliday = attendanceDao.getAllAttendanceRecords().first()
                .any { it.date == date.toEpochDay() && it.type == RecordType.HOLIDAY }

            if (isAlreadyHoliday) {
                attendanceDao.deleteHolidayOnDate(date.toEpochDay())
            } else {
                _showHolidayDialog.value = date
            }
        }
    }

    fun onHolidayToggleConfirmed() {
        viewModelScope.launch {
            _showHolidayDialog.value?.let { date ->
                val dateAsLong = date.toEpochDay()
                attendanceDao.deleteAttendanceRecordsOnDate(dateAsLong)
                val holidayRecord = AttendanceRecord(
                    subjectId = 0,
                    scheduleId = -2L,
                    date = dateAsLong,
                    isPresent = false,
                    note = "Holiday",
                    type = RecordType.HOLIDAY
                )
                attendanceDao.insertAttendanceRecord(holidayRecord)

                val dayOfWeek = date.dayOfWeek.value
                val schedulesForDay = attendanceDao.getSchedulesForDayNow(dayOfWeek)
                schedulesForDay.forEach { schedule ->
                    AlarmScheduler.cancelClassAlarm(applicationContext, schedule)
                }
            }
            _showHolidayDialog.value = null
        }
    }

    fun onHolidayToggleDismissed() {
        _showHolidayDialog.value = null
    }

    fun addManualAttendance(subjectId: Long, presentCount: Int, absentCount: Int) {
        viewModelScope.launch {
            val manualRecords = mutableListOf<AttendanceRecord>()
            val note = "Manually Added"
            var pseudoDate = System.currentTimeMillis() * -1

            repeat(presentCount) {
                manualRecords.add(
                    AttendanceRecord(
                        subjectId = subjectId,
                        scheduleId = -3L,
                        date = pseudoDate--,
                        isPresent = true,
                        note = note,
                        type = RecordType.MANUAL
                    )
                )
            }

            repeat(absentCount) {
                manualRecords.add(
                    AttendanceRecord(
                        subjectId = subjectId,
                        scheduleId = -3L,
                        date = pseudoDate--,
                        isPresent = false,
                        note = note,
                        type = RecordType.MANUAL
                    )
                )
            }

            manualRecords.forEach { attendanceDao.insertAttendanceRecord(it) }
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

    suspend fun calculateBunkAnalysis(subjectId: Long): BunkAnalysis {
        val subject = getSubjectById(subjectId) ?: return BunkAnalysis(0, 0)
        val attended = attendanceDao.getPresentClassesForSubject(subjectId)
        val total = attendanceDao.getTotalClassesForSubject(subjectId)
        val target = subject.targetAttendance.toDouble()

        if (total == 0) return BunkAnalysis(0, 0)

        val currentPercentage = (attended.toDouble() / total) * 100.0

        return if (currentPercentage >= target) {
            var bunksAllowed = 0
            while (true) {
                val futureTotal = total + 1 + bunksAllowed
                val futurePercentage = (attended.toDouble() / futureTotal) * 100
                if (futurePercentage < target) {
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
            val isTodayHoliday = records.any { it.date == todayEpochDay && it.type == RecordType.HOLIDAY }
            if (isTodayHoliday) {
                emptyList()
            } else {
                schedules.mapNotNull { schedule ->
                    subjects.find { it.id == schedule.subjectId }
                        ?.let { ScheduleWithSubject(schedule, it) }
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
}
