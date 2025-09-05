package com.ankit.smartattendance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // --- Subject Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

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

    @Query("SELECT * FROM class_schedules WHERE subjectId = :subjectId")
    suspend fun getSchedulesForSubject(subjectId: Long): List<ClassSchedule>

    @Query("DELETE FROM class_schedules WHERE subjectId = :subjectId")
    suspend fun deleteSchedulesForSubject(subjectId: Long)

    @Query("SELECT * FROM class_schedules WHERE dayOfWeek = :dayOfWeek")
    fun getSchedulesForDay(dayOfWeek: Int): Flow<List<ClassSchedule>>


    // --- Attendance Record Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId")
    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>>

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND scheduleId = :scheduleId AND date = :date")
    suspend fun countClassRecordsForDay(subjectId: Long, scheduleId: Long, date: Long): Int

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND date = :date")
    suspend fun deleteRecordsForSubjectOnDate(subjectId: Long, date: Long)

    // **NEW**: Query to delete only the regular (scheduled) class for a specific day.
    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND scheduleId IS NOT NULL")
    suspend fun deleteRegularRecordForDate(subjectId: Long, date: Long)

    // **NEW**: Query to delete only extra classes for a specific day.
    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND scheduleId IS NULL")
    suspend fun deleteExtraClassRecordForDate(subjectId: Long, date: Long)

    @Query("DELETE FROM attendance_records WHERE date = :date AND type = 'HOLIDAY'")
    suspend fun deleteHolidayOnDate(date: Long)

    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteAttendanceRecordsOnDate(date: Long)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND type = 'MANUAL'")
    suspend fun deleteManualRecordsForSubject(subjectId: Long)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllAttendanceRecords()

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteAttendanceRecordsForSubject(subjectId: Long)


    // --- Statistics Queries ---
    @Query("SELECT COUNT(*) FROM attendance_records WHERE (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getTotalClassesOverall(): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE (type = 'CLASS' OR type = 'MANUAL') AND isPresent = 1")
    suspend fun getTotalPresentOverall(): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getTotalClassesForSubject(subjectId: Long): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND isPresent = 1 AND (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getPresentClassesForSubject(subjectId: Long): Int
}
