package com.ankit.smartattendance.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ankit.smartattendance.models.AttendanceRecordWithSubject
import com.ankit.smartattendance.models.SubjectWithAttendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // --- Subject Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getSubjectById(subjectId: Long): Subject?

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()


    // --- Schedule Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ClassSchedule)

    @Query("SELECT * FROM class_schedules")
    fun getAllSchedules(): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedules WHERE subjectId = :subjectId")
    suspend fun getSchedulesForSubject(subjectId: Long): List<ClassSchedule>

    @Query("DELETE FROM class_schedules WHERE subjectId = :subjectId")
    suspend fun deleteSchedulesForSubject(subjectId: Long)

    @Query("SELECT * FROM class_schedules WHERE dayOfWeek = :dayOfWeek")
    fun getSchedulesForDay(dayOfWeek: Int): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedules WHERE dayOfWeek = :dayOfWeek")
    suspend fun getSchedulesForDayNow(dayOfWeek: Int): List<ClassSchedule>

    @Query("DELETE FROM class_schedules")
    suspend fun deleteAllSchedules()


    // --- Attendance Record Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendanceRecord(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE scheduleId = :scheduleId AND date = :date")
    suspend fun getRecordByScheduleIdAndDate(scheduleId: Long, date: Long): AttendanceRecord?

    @Query("DELETE FROM attendance_records WHERE date = :date AND type = 'HOLIDAY'")
    suspend fun deleteHolidayOnDate(date: Long)

    @Query("DELETE FROM attendance_records WHERE date = :date AND type != 'HOLIDAY'")
    suspend fun deleteAttendanceRecordsOnDate(date: Long)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteAttendanceRecordsForSubject(subjectId: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllAttendanceRecords()

    // --- Statistics & Joined Queries ---
    @Query("SELECT COUNT(*) FROM attendance_records WHERE (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getTotalClassesOverall(): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE isPresent = 1 AND (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getTotalPresentOverall(): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getTotalClassesForSubject(subjectId: Long): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND isPresent = 1 AND (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getPresentClassesForSubject(subjectId: Long): Int

    @Transaction
    @Query("""
        SELECT 
            s.*,
            (SELECT COUNT(*) FROM attendance_records WHERE subjectId = s.id AND (type = 'CLASS' OR type = 'MANUAL')) as totalClasses,
            (SELECT COUNT(*) FROM attendance_records WHERE subjectId = s.id AND isPresent = 1 AND (type = 'CLASS' OR type = 'MANUAL')) as presentClasses
        FROM subjects s
    """)
    fun getSubjectsWithAttendance(): Flow<List<SubjectWithAttendance>>

    @Transaction
    @Query("""
        SELECT ar.*, s.name as subjectName, s.color as subjectColor
        FROM attendance_records ar 
        LEFT JOIN subjects s ON ar.subjectId = s.id 
        WHERE ar.date = :date AND ar.type != 'HOLIDAY'
    """)
    fun getRecordsForDateWithSubject(date: Long): Flow<List<AttendanceRecordWithSubject>>
}
