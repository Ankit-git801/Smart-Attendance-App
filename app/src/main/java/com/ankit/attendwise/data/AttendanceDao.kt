/*
 * Copyright (c) 2026 Ankit. All rights reserved.
 */

package com.ankit.attendwise.data

import androidx.room.*
import com.ankit.attendwise.models.AttendanceRecordWithSubject
import com.ankit.attendwise.models.SubjectWithAttendance
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

    // --- Subject Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :subjectId")
    suspend fun getSubjectById(subjectId: String): Subject?

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()


    // --- Schedule Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ClassSchedule)

    @Delete
    suspend fun deleteSchedule(schedule: ClassSchedule)

    @Query("SELECT * FROM class_schedules")
    fun getAllSchedules(): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedules WHERE subjectId = :subjectId")
    suspend fun getSchedulesForSubject(subjectId: String): List<ClassSchedule>

    @Query("DELETE FROM class_schedules WHERE subjectId = :subjectId")
    suspend fun deleteSchedulesForSubject(subjectId: String)

    @Query("SELECT * FROM class_schedules WHERE dayOfWeek = :dayOfWeek")
    fun getSchedulesForDay(dayOfWeek: Int): Flow<List<ClassSchedule>>

    @Query("SELECT * FROM class_schedules WHERE dayOfWeek = :dayOfWeek")
    suspend fun getSchedulesForDayNow(dayOfWeek: Int): List<ClassSchedule>

    @Query("DELETE FROM class_schedules")
    suspend fun deleteAllSchedules()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<Subject>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<ClassSchedule>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecord>)

    @Transaction
    suspend fun restoreDataBatch(
        subjects: List<Subject>,
        schedules: List<ClassSchedule>,
        records: List<AttendanceRecord>
    ) {
        deleteAllSubjects()
        deleteAllSchedules()
        deleteAllAttendanceRecords()
        insertSubjects(subjects)
        insertSchedules(schedules)
        insertAttendanceRecords(records)
    }


    // --- Attendance Record Queries ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteAttendanceRecord(record: AttendanceRecord)

    @Query("SELECT * FROM attendance_records")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE id = :id")
    suspend fun getAttendanceRecordById(id: String): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getAttendanceRecordsForSubject(subjectId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE scheduleId = :scheduleId AND date = :date")
    suspend fun getRecordByScheduleIdAndDate(scheduleId: String, date: Long): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND type = 'MANUAL' LIMIT 1")
    suspend fun getManualRecordForDateAndSubject(subjectId: String, date: Long): AttendanceRecord?

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND type != 'HOLIDAY'")
    suspend fun getAttendanceRecordsForSubjectOnDate(subjectId: String, date: Long): List<AttendanceRecord>

    @Query("DELETE FROM attendance_records WHERE date = :date AND type = 'HOLIDAY'")
    suspend fun deleteHolidayOnDate(date: Long)

    @Query("DELETE FROM attendance_records WHERE date = :date AND type != 'HOLIDAY'")
    suspend fun deleteAttendanceRecordsOnDate(date: Long)

    @Query("SELECT * FROM attendance_records WHERE date = :date")
    suspend fun getAllAttendanceRecordsOnDateNow(date: Long): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE date = :date AND type != 'HOLIDAY'")
    suspend fun getAttendanceRecordsOnDateNow(date: Long): List<AttendanceRecord>


    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND scheduleId = :scheduleId AND type != 'HOLIDAY'")
    suspend fun deleteAttendanceRecordBySchedule(subjectId: String, date: Long, scheduleId: String)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND type != 'HOLIDAY'")
    suspend fun deleteAttendanceRecordsForSubjectOnDate(subjectId: String, date: Long)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteAttendanceRecordsForSubject(subjectId: String)

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllAttendanceRecords()


    // --- Statistics & Joined Queries ---
    @Query("SELECT COUNT(*) FROM attendance_records WHERE type = 'CLASS' OR type = 'MANUAL'")
    suspend fun getTotalClassesOverall(): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE isPresent = 1 AND (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getTotalPresentOverall(): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getTotalClassesForSubject(subjectId: String): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND isPresent = 1 AND (type = 'CLASS' OR type = 'MANUAL')")
    suspend fun getPresentClassesForSubject(subjectId: String): Int

    @Transaction
    @Query("""
        SELECT s.*,
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
