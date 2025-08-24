package com.ankit.smartattendance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    // --- Subject Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long
    @Update
    suspend fun updateSubject(subject: Subject)
    @Delete
    suspend fun deleteSubject(subject: Subject)
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>
    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getSubjectById(subjectId: Long): Subject?
    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()

    // --- Class Schedule Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ClassSchedule)
    @Query("DELETE FROM class_schedules WHERE subjectId = :subjectId")
    suspend fun deleteSchedulesForSubject(subjectId: Long)
    @Query("SELECT * FROM class_schedules WHERE subjectId = :subjectId")
    suspend fun getSchedulesForSubject(subjectId: Long): List<ClassSchedule>
    @Query("SELECT * FROM class_schedules WHERE dayOfWeek = :dayOfWeek")
    fun getSchedulesForDay(dayOfWeek: Int): Flow<List<ClassSchedule>>

    // --- Attendance Record Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord)
    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId")
    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>>
    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun getTotalClassesForSubject(subjectId: Long): Int
    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND isPresent = 1")
    suspend fun getPresentClassesForSubject(subjectId: Long): Int
    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllAttendanceRecords()
    @Query("DELETE FROM attendance_records WHERE date = :date AND type = 'HOLIDAY'")
    suspend fun deleteHolidayOnDate(date: Long)
    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND type = 'MANUAL'")
    suspend fun deleteManualRecordsForSubject(subjectId: Long)

    // NEW FUNCTION TO CHECK IF ATTENDANCE FOR A SCHEDULE IS MARKED ON A GIVEN DATE
    @Query("SELECT EXISTS(SELECT 1 FROM attendance_records WHERE scheduleId = :scheduleId AND date = :date LIMIT 1)")
    fun isAttendanceMarkedForSchedule(scheduleId: Long, date: Long): Flow<Boolean>

    // --- Complex / Joined Queries for Statistics ---
    @Query("SELECT COUNT(*) FROM attendance_records")
    suspend fun getTotalClassesOverall(): Int
    @Query("SELECT COUNT(*) FROM attendance_records WHERE isPresent = 1")
    suspend fun getTotalPresentOverall(): Int
    @Query("SELECT COUNT(DISTINCT id) FROM subjects")
    suspend fun getSubjectCount(): Int
}
