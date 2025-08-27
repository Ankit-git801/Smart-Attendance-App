package com.ankit.smartattendance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankit.smartattendance.data.*
import com.ankit.smartattendance.models.AttendanceStatistics
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.models.SubjectWithAttendance
import com.ankit.smartattendance.utils.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
    private val preferencesManager = PreferencesManager(application)
    private val applicationContext = application.applicationContext

    val allSubjects: Flow<List<Subject>> = attendanceDao.getAllSubjects()
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceRecords()

    val subjectsWithAttendance: StateFlow<List<SubjectWithAttendance>> = allSubjects.map { subjects ->
        subjects.map { subject ->
            val percentage = getAttendancePercentage(subject.id)
            SubjectWithAttendance(subject, percentage)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


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

    // This is now correctly filtered for holidays
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
            attendanceDao.deleteSubject(subject)
        }
    }

    fun markAttendance(subjectId: Long, scheduleId: Long?, isPresent: Boolean) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            if (scheduleId != null) {
                if (attendanceDao.countClassRecordsForDay(subjectId, scheduleId, today) > 0) {
                    return@launch
                }
            }
            val record = AttendanceRecord(
                subjectId = subjectId,
                scheduleId = scheduleId,
                date = today,
                isPresent = isPresent
            )
            attendanceDao.insertAttendanceRecord(record)
        }
    }

    fun markAttendanceForDate(subjectId: Long, date: LocalDate, isPresent: Boolean?) {
        viewModelScope.launch {
            val dateAsLong = date.toEpochDay()
            attendanceDao.deleteRecordForDate(subjectId, dateAsLong)

            if (isPresent != null) {
                val newRecord = AttendanceRecord(
                    subjectId = subjectId,
                    scheduleId = null,
                    date = dateAsLong,
                    isPresent = isPresent,
                    type = RecordType.CLASS
                )
                attendanceDao.insertAttendanceRecord(newRecord)
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
                    subjectId = null,
                    scheduleId = null,
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

    fun markExtraClassAttendance(subjectId: Long, isPresent: Boolean, note: String?) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                subjectId = subjectId,
                scheduleId = null,
                date = LocalDate.now().toEpochDay(),
                isPresent = isPresent,
                note = note ?: "Extra Class"
            )
            attendanceDao.insertAttendanceRecord(record)
            hideExtraClassDialog()
        }
    }

    fun addManualAttendance(subjectId: Long, presentCount: Int, absentCount: Int) {
        viewModelScope.launch {
            attendanceDao.deleteManualRecordsForSubject(subjectId)
            val manualRecords = mutableListOf<AttendanceRecord>()
            val note = "Manually Added"
            repeat(presentCount) {
                manualRecords.add(
                    AttendanceRecord(
                        subjectId = subjectId, scheduleId = null, date = 0,
                        isPresent = true, note = note, type = RecordType.MANUAL
                    )
                )
            }
            repeat(absentCount) {
                manualRecords.add(
                    AttendanceRecord(
                        subjectId = subjectId, scheduleId = null, date = 0,
                        isPresent = false, note = note, type = RecordType.MANUAL
                    )
                )
            }
            manualRecords.forEach { attendanceDao.insertAttendanceRecord(it) }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val subjects = allSubjects.first()
            subjects.forEach { subject ->
                val schedules = attendanceDao.getSchedulesForSubject(subject.id)
                AlarmScheduler.cancelClassAlarms(applicationContext, schedules)
            }
            attendanceDao.deleteAllSubjects()
        }
    }

    suspend fun getSubjectById(subjectId: Long): Subject? = attendanceDao.getSubjectById(subjectId)
    suspend fun getSchedulesForSubject(subjectId: Long): List<ClassSchedule> =
        attendanceDao.getSchedulesForSubject(subjectId)

    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>> =
        attendanceDao.getAttendanceRecordsForSubject(subjectId)

    /**
     * THE FIX IS HERE.
     * This function now combines three sources: the day's schedules, the list of all subjects,
     * and the list of all attendance records. It then checks if today is a holiday. If it is,
     * it returns an empty list. Otherwise, it returns the normally scheduled classes.
     */
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
                emptyList() // If it's a holiday, return an empty list of classes.
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

    suspend fun getAttendancePercentage(subjectId: Long): Double {
        val total = attendanceDao.getTotalClassesForSubject(subjectId)
        val present = attendanceDao.getPresentClassesForSubject(subjectId)
        return if (total > 0) (present.toDouble() / total) * 100 else 0.0
    }

    suspend fun getTotalClassesForSubject(subjectId: Long): Int =
        attendanceDao.getTotalClassesForSubject(subjectId)

    suspend fun getPresentClassesForSubject(subjectId: Long): Int =
        attendanceDao.getPresentClassesForSubject(subjectId)
}
