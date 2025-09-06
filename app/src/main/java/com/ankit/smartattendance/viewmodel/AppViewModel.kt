package com.ankit.smartattendance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankit.smartattendance.data.*
import com.ankit.smartattendance.models.AttendanceStatistics
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.models.SubjectWithAttendance
import com.ankit.smartattendance.utils.AlarmScheduler
import com.ankit.smartattendance.utils.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.atomic.AtomicLong

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

    private val _showExtraClassDialog = MutableStateFlow(false)
    val showExtraClassDialog: StateFlow<Boolean> = _showExtraClassDialog.asStateFlow()

    val todaysScheduleWithSubjects: StateFlow<List<ScheduleWithSubject>> =
        getTodaysSchedule().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    private val _showHolidayDialog = MutableStateFlow<LocalDate?>(null)
    val showHolidayDialog: StateFlow<LocalDate?> = _showHolidayDialog.asStateFlow()

    fun setTheme(theme: String) {
        viewModelScope.launch { preferencesManager.saveTheme(theme) }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            preferencesManager.saveUserName(name)
        }
    }

    fun showExtraClassDialog() {
        _showExtraClassDialog.value = true
    }

    fun hideExtraClassDialog() {
        _showExtraClassDialog.value = false
    }

    fun addOrUpdateSubject(subject: Subject, schedules: List<ClassSchedule>) {
        viewModelScope.launch {
            val oldSchedules = attendanceDao.getSchedulesForSubject(subject.id)
            AlarmScheduler.cancelClassAlarms(applicationContext, oldSchedules)
            val subjectId = attendanceDao.insertSubject(subject)
            attendanceDao.deleteSchedulesForSubject(subjectId)
            schedules.forEach { attendanceDao.insertSchedule(it.copy(subjectId = subjectId)) }
            val newSchedules = attendanceDao.getSchedulesForSubject(subjectId)
            AlarmScheduler.scheduleClassAlarms(
                applicationContext,
                subject.copy(id = subjectId),
                newSchedules
            )
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            val schedules = attendanceDao.getSchedulesForSubject(subject.id)
            AlarmScheduler.cancelClassAlarms(applicationContext, schedules)
            // THIS IS THE FIX: Delete attendance records before deleting the subject.
            attendanceDao.deleteAttendanceRecordsForSubject(subject.id)
            attendanceDao.deleteSubject(subject)
        }
    }

    private fun checkAttendanceAndWarn(subjectId: Long) {
        viewModelScope.launch {
            val subject = attendanceDao.getSubjectById(subjectId)
            if (subject != null) {
                val total = attendanceDao.getTotalClassesForSubject(subjectId)
                val present = attendanceDao.getPresentClassesForSubject(subjectId)
                val percentage = if (total > 0) (present.toDouble() / total) * 100.0 else 0.0

                if (percentage < subject.targetAttendance) {
                    NotificationHelper.showAttendanceWarningNotification(applicationContext, subject, percentage)
                }
            }
        }
    }

    private fun markAttendance(
        subjectId: Long,
        scheduleId: Long,
        date: LocalDate,
        type: RecordType,
        isPresent: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                subjectId = subjectId,
                scheduleId = scheduleId,
                date = date.toEpochDay(),
                isPresent = isPresent,
                type = type,
                note = note
            )
            attendanceDao.insertAttendanceRecord(record)
            if (!isPresent) {
                checkAttendanceAndWarn(subjectId)
            }
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

    fun markAsPresentForDate(subjectId: Long, date: LocalDate) {
        viewModelScope.launch {
            val scheduleForDay = attendanceDao.getSchedulesForSubject(subjectId).find { it.dayOfWeek == date.dayOfWeek.value + 1 }
            val scheduleId = scheduleForDay?.id ?: -1L
            markAttendance(subjectId, scheduleId, date, RecordType.CLASS, true, "Marked from Calendar")
        }
    }

    fun markAsAbsentForDate(subjectId: Long, date: LocalDate) {
        viewModelScope.launch {
            val scheduleForDay = attendanceDao.getSchedulesForSubject(subjectId).find { it.dayOfWeek == date.dayOfWeek.value + 1 }
            val scheduleId = scheduleForDay?.id ?: -1L
            markAttendance(subjectId, scheduleId, date, RecordType.CLASS, false, "Marked from Calendar")
        }
    }

    fun markAsCancelledForDate(subjectId: Long, date: LocalDate) {
        viewModelScope.launch {
            val scheduleForDay = attendanceDao.getSchedulesForSubject(subjectId).find { it.dayOfWeek == date.dayOfWeek.value + 1 }
            val scheduleId = scheduleForDay?.id ?: -1L
            markAttendance(subjectId, scheduleId, date, RecordType.CANCELLED, false, "Marked from Calendar")
        }
    }

    fun markExtraClassAttendanceForDate(subjectId: Long, date: LocalDate) {
        markAttendance(subjectId, 0L, date, RecordType.CLASS, true, "Extra Class from Calendar")
    }

    fun markExtraClassAttendance(subjectId: Long, isPresent: Boolean, note: String?) {
        markAttendance(subjectId, 0L, LocalDate.now(), RecordType.CLASS, isPresent, note ?: "Extra Class")
        hideExtraClassDialog()
    }

    fun clearRegularAttendanceForDate(subjectId: Long, date: LocalDate) {
        viewModelScope.launch {
            attendanceDao.deleteRegularRecordForDate(subjectId, date.toEpochDay())
        }
    }

    fun clearExtraClassAttendanceForDate(subjectId: Long, date: LocalDate) {
        viewModelScope.launch {
            attendanceDao.deleteExtraClassRecordForDate(subjectId, date.toEpochDay())
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
            val dateCounter = AtomicLong(System.nanoTime() * -1)

            repeat(presentCount) {
                manualRecords.add(
                    AttendanceRecord(
                        subjectId = subjectId,
                        scheduleId = -3L,
                        date = dateCounter.getAndIncrement(),
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
                        date = dateCounter.getAndIncrement(),
                        isPresent = false,
                        note = note,
                        type = RecordType.MANUAL
                    )
                )
            }
            manualRecords.forEach { attendanceDao.insertAttendanceRecord(it) }
            checkAttendanceAndWarn(subjectId)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val subjects = allSubjects.first()
            subjects.forEach { subject ->
                val schedules = attendanceDao.getSchedulesForSubject(subject.id)
                AlarmScheduler.cancelClassAlarms(applicationContext, schedules)
            }
            // THIS IS THE FIX: Delete all attendance records before deleting all subjects.
            attendanceDao.deleteAllAttendanceRecords()
            attendanceDao.deleteAllSubjects()
        }
    }

    suspend fun getSubjectById(subjectId: Long): Subject? = attendanceDao.getSubjectById(subjectId)

    suspend fun getSchedulesForSubject(subjectId: Long): List<ClassSchedule> =
        attendanceDao.getSchedulesForSubject(subjectId)

    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceRecordsForSubject(subjectId)

    suspend fun calculateBunkAnalysis(subjectId: Long): BunkAnalysis {
        val subject = getSubjectById(subjectId) ?: return BunkAnalysis(0, 0)
        var attended = attendanceDao.getPresentClassesForSubject(subjectId)
        var total = attendanceDao.getTotalClassesForSubject(subjectId)
        val target = subject.targetAttendance.toDouble()

        if (total == 0) {
            return BunkAnalysis(0, 0)
        }

        val currentPercentage = (attended.toDouble() / total) * 100.0

        return if (currentPercentage >= target) {
            var bunksAllowed = 0
            while (true) {
                val futureTotal = total + 1
                val futurePercentage = (attended.toDouble() / futureTotal) * 100
                if (futurePercentage < target) {
                    break
                }
                total++
                bunksAllowed++
            }
            BunkAnalysis(classesToBunk = bunksAllowed, classesToAttend = 0)
        } else {
            var mustAttend = 0
            while (true) {
                if (total + mustAttend == 0) {
                    mustAttend++
                    continue
                }
                val futurePercentage = ((attended + mustAttend).toDouble() / (total + mustAttend)) * 100
                if (futurePercentage >= target) {
                    break
                }
                mustAttend++
                if (mustAttend > total * 2) break
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
}
