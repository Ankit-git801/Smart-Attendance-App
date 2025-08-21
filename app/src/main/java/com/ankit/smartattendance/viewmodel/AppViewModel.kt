package com.ankit.smartattendance.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ankit.smartattendance.data.AppDatabase
import com.ankit.smartattendance.data.AttendanceRecord
import com.ankit.smartattendance.data.ClassSchedule
import com.ankit.smartattendance.data.Subject
import com.ankit.smartattendance.models.AttendanceStatistics
import com.ankit.smartattendance.models.ScheduleWithSubject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.Calendar

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // CORRECTED: Initialize attendanceDao FIRST.
    private val attendanceDao = AppDatabase.getDatabase(application).attendanceDao()

    // Now, other properties can safely use attendanceDao.
    val allAttendanceRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllAttendanceRecords()

    // --- StateFlows for UI ---

    val allSubjects: Flow<List<Subject>> = attendanceDao.getAllSubjects()

    private val _showExtraClassDialog = MutableStateFlow(false)
    val showExtraClassDialog: StateFlow<Boolean> = _showExtraClassDialog.asStateFlow()

    private val _currentTime = MutableStateFlow(LocalTime.now())
    val currentTime: StateFlow<LocalTime> = _currentTime.asStateFlow()

    val todaysScheduleWithSubjects: StateFlow<List<ScheduleWithSubject>> =
        getTodaysSchedule().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // This will be used to update the time every second for the UI
        // We will implement this later with the Alarm/Notification scheduler
    }


    // --- UI Actions ---

    fun showExtraClassDialog() {
        _showExtraClassDialog.value = true
    }

    fun hideExtraClassDialog() {
        _showExtraClassDialog.value = false
    }


    // --- Database Operations ---

    fun addOrUpdateSubject(subject: Subject, schedules: List<ClassSchedule>) {
        viewModelScope.launch {
            val subjectId = attendanceDao.insertSubject(subject)
            attendanceDao.deleteSchedulesForSubject(subjectId)
            schedules.forEach { schedule ->
                attendanceDao.insertSchedule(schedule.copy(subjectId = subjectId))
            }
        }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch {
            attendanceDao.deleteSubject(subject)
        }
    }

    fun markAttendance(subjectId: Long, scheduleId: Long?, isPresent: Boolean) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                subjectId = subjectId,
                scheduleId = scheduleId,
                date = LocalDate.now().toEpochDay(),
                isPresent = isPresent
            )
            attendanceDao.insertAttendanceRecord(record)
        }
    }

    fun markExtraClassAttendance(subjectId: Long, isPresent: Boolean, note: String?) {
        viewModelScope.launch {
            val record = AttendanceRecord(
                subjectId = subjectId,
                scheduleId = null, // Null for extra classes
                date = LocalDate.now().toEpochDay(),
                isPresent = isPresent,
                note = note
            )
            attendanceDao.insertAttendanceRecord(record)
            hideExtraClassDialog()
        }
    }


    // --- Data Fetching and Processing ---

    suspend fun getSubjectById(subjectId: Long): Subject? {
        return attendanceDao.getSubjectById(subjectId)
    }

    suspend fun getSchedulesForSubject(subjectId: Long): List<ClassSchedule> {
        return attendanceDao.getSchedulesForSubject(subjectId)
    }

    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>> {
        return attendanceDao.getAttendanceRecordsForSubject(subjectId)
    }

    private fun getTodaysSchedule(): Flow<List<ScheduleWithSubject>> {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return attendanceDao.getSchedulesForDay(today).combine(allSubjects) { schedules, subjects ->
            schedules.mapNotNull { schedule ->
                val subject = subjects.find { it.id == schedule.subjectId }
                if (subject != null) {
                    ScheduleWithSubject(schedule, subject)
                } else {
                    null
                }
            }.sortedBy { it.schedule.startHour }
        }
    }

    suspend fun getOverallStatistics(): AttendanceStatistics {
        val totalClasses = attendanceDao.getTotalClassesOverall()
        val totalPresent = attendanceDao.getTotalPresentOverall()
        val subjectCount = attendanceDao.getSubjectCount()
        val percentage = if (totalClasses > 0) (totalPresent.toDouble() / totalClasses) * 100 else 0.0

        return AttendanceStatistics(
            totalClasses = totalClasses,
            totalPresent = totalPresent,
            totalAbsent = totalClasses - totalPresent,
            overallPercentage = percentage,
            subjectCount = subjectCount
        )
    }

    suspend fun getAttendancePercentage(subjectId: Long): Double {
        val total = attendanceDao.getTotalClassesForSubject(subjectId)
        val present = attendanceDao.getPresentClassesForSubject(subjectId)
        return if (total > 0) (present.toDouble() / total) * 100 else 100.0
    }

    suspend fun getTotalClassesForSubject(subjectId: Long): Int {
        return attendanceDao.getTotalClassesForSubject(subjectId)
    }

    suspend fun getPresentClassesForSubject(subjectId: Long): Int {
        return attendanceDao.getPresentClassesForSubject(subjectId)
    }

    suspend fun getAbsentClassesForSubject(subjectId: Long): Int {
        val total = attendanceDao.getTotalClassesForSubject(subjectId)
        val present = attendanceDao.getPresentClassesForSubject(subjectId)
        return total - present
    }
}
