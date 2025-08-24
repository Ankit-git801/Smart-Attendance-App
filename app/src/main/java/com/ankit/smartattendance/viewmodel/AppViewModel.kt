package com.ankit.smartattendance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankit.smartattendance.data.*
import com.ankit.smartattendance.models.AttendanceStatistics
import com.ankit.smartattendance.models.ScheduleWithSubject
import com.ankit.smartattendance.utils.AlarmScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()
    private val preferencesManager = PreferencesManager(application)
    private val applicationContext = application.applicationContext

    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceRecords()
    val theme: StateFlow<String> = preferencesManager.themeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System Default")
    val allSubjects: Flow<List<Subject>> = attendanceDao.getAllSubjects()
    private val _showExtraClassDialog = MutableStateFlow(false)
    val showExtraClassDialog: StateFlow<Boolean> = _showExtraClassDialog.asStateFlow()
    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime: StateFlow<LocalTime> = _currentTime.asStateFlow()
    val todaysScheduleWithSubjects: StateFlow<List<ScheduleWithSubject>> = getTodaysSchedule().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTheme(theme: String) {
        viewModelScope.launch { preferencesManager.saveTheme(theme) }
    }

    fun showExtraClassDialog() { _showExtraClassDialog.value = true }
    fun hideExtraClassDialog() { _showExtraClassDialog.value = false }

    fun addOrUpdateSubject(subject: Subject, schedules: List<ClassSchedule>) {
        viewModelScope.launch {
            val oldSchedules = attendanceDao.getSchedulesForSubject(subject.id)
            AlarmScheduler.cancelClassAlarms(applicationContext, oldSchedules)
            val subjectId = attendanceDao.insertSubject(subject)
            attendanceDao.deleteSchedulesForSubject(subjectId)
            schedules.forEach { attendanceDao.insertSchedule(it.copy(subjectId = subjectId)) }
            val newSchedules = attendanceDao.getSchedulesForSubject(subjectId)
            AlarmScheduler.scheduleClassAlarms(applicationContext, subject.copy(id = subjectId), newSchedules)
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
            val record = AttendanceRecord(
                subjectId = subjectId,
                scheduleId = scheduleId,
                date = LocalDate.now().toEpochDay(),
                isPresent = isPresent,
                note = "Marked from Home"
            )
            attendanceDao.insertAttendanceRecord(record)
        }
    }

    fun toggleHoliday(date: LocalDate) {
        viewModelScope.launch {
            val dateAsLong = date.toEpochDay()
            val existingHoliday = attendanceDao.getAllAttendanceRecords().first()
                .find { it.date == dateAsLong && it.type == RecordType.HOLIDAY }
            if (existingHoliday != null) {
                attendanceDao.deleteHolidayOnDate(dateAsLong)
            } else {
                val holidayRecord = AttendanceRecord(subjectId = null, scheduleId = null, date = dateAsLong, isPresent = false, note = "Holiday", type = RecordType.HOLIDAY)
                attendanceDao.insertAttendanceRecord(holidayRecord)
            }
        }
    }

    fun markExtraClassAttendance(subjectId: Long, isPresent: Boolean, note: String?) {
        viewModelScope.launch {
            val record = AttendanceRecord(subjectId = subjectId, scheduleId = null, date = LocalDate.now().toEpochDay(), isPresent = isPresent, note = note ?: "Extra Class")
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
                manualRecords.add(AttendanceRecord(
                    subjectId = subjectId, scheduleId = null, date = 0,
                    isPresent = true, note = note, type = RecordType.MANUAL
                ))
            }
            repeat(absentCount) {
                manualRecords.add(AttendanceRecord(
                    subjectId = subjectId, scheduleId = null, date = 0,
                    isPresent = false, note = note, type = RecordType.MANUAL
                ))
            }
            manualRecords.forEach { attendanceDao.insertAttendanceRecord(it) }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            attendanceDao.deleteAllSubjects()
            attendanceDao.deleteAllAttendanceRecords()
        }
    }

    // NEW FUNCTION
    fun isAttendanceMarkedForToday(scheduleId: Long): Flow<Boolean> {
        val today = LocalDate.now().toEpochDay()
        return attendanceDao.isAttendanceMarkedForSchedule(scheduleId, today)
    }

    suspend fun getSubjectById(subjectId: Long): Subject? = attendanceDao.getSubjectById(subjectId)
    suspend fun getSchedulesForSubject(subjectId: Long): List<ClassSchedule> = attendanceDao.getSchedulesForSubject(subjectId)
    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>> = attendanceDao.getAttendanceRecordsForSubject(subjectId)

    private fun getTodaysSchedule(): Flow<List<ScheduleWithSubject>> {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return attendanceDao.getSchedulesForDay(today).combine(allSubjects) { schedules, subjects ->
            schedules.mapNotNull { schedule ->
                subjects.find { it.id == schedule.subjectId }?.let { ScheduleWithSubject(schedule, it) }
            }.sortedBy { it.schedule.startHour }
        }
    }

    suspend fun getOverallStatistics(): AttendanceStatistics {
        val total = attendanceDao.getTotalClassesOverall()
        val present = attendanceDao.getTotalPresentOverall()
        val subjects = attendanceDao.getSubjectCount()
        return AttendanceStatistics(total, present, total - present, if (total > 0) (present.toDouble() / total) * 100 else 0.0, subjects)
    }

    suspend fun getAttendancePercentage(subjectId: Long): Double {
        val total = attendanceDao.getTotalClassesForSubject(subjectId)
        val present = attendanceDao.getPresentClassesForSubject(subjectId)
        return if (total > 0) (present.toDouble() / total) * 100 else 100.0
    }

    suspend fun getTotalClassesForSubject(subjectId: Long): Int = attendanceDao.getTotalClassesForSubject(subjectId)
    suspend fun getPresentClassesForSubject(subjectId: Long): Int = attendanceDao.getPresentClassesForSubject(subjectId)
    suspend fun getAbsentClassesForSubject(subjectId: Long): Int = attendanceDao.getTotalClassesForSubject(subjectId) - attendanceDao.getPresentClassesForSubject(subjectId)
}
