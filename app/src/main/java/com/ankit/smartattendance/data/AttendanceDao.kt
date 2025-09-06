package com.ankit.smartattendance.data

import androidx.room.*
import com.ankit.smartattendance.models.SubjectWithAttendance
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

    @Query("SELECT * FROM subjects")
    suspend fun getAllSubjectsNow(): List<Subject>

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

    // THIS IS THE FIX: New query to delete attendance records for a specific subject.
    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId")
    suspend fun deleteAttendanceRecordsForSubject(subjectId: Long)

    // THIS IS THE FIX: New query to delete all attendance records.
    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllAttendanceRecords()

    @Query("SELECT * FROM attendance_records WHERE subjectId = :subjectId ORDER BY date DESC")
    fun getAttendanceRecordsForSubject(subjectId: Long): Flow<List<AttendanceRecord>>

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND scheduleId != 0")
    suspend fun deleteRegularRecordForDate(subjectId: Long, date: Long)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND date = :date AND scheduleId = 0")
    suspend fun deleteExtraClassRecordForDate(subjectId: Long, date: Long)

    @Query("DELETE FROM attendance_records WHERE date = :date AND type = 'HOLIDAY'")
    suspend fun deleteHolidayOnDate(date: Long)

    @Query("DELETE FROM attendance_records WHERE date = :date")
    suspend fun deleteAttendanceRecordsOnDate(date: Long)

    @Query("DELETE FROM attendance_records WHERE subjectId = :subjectId AND type = 'MANUAL'")
    suspend fun deleteManualRecordsForSubject(subjectId: Long)

    // --- Statistics Queries (Corrected to ignore cancelled classes) ---
    @Query("SELECT COUNT(*) FROM attendance_records WHERE (type = 'CLASS' OR type = 'MANUAL') AND type != 'CANCELLED'")
    suspend fun getTotalClassesOverall(): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE (type = 'CLASS' OR type = 'MANUAL') AND isPresent = 1 AND type != 'CANCELLED'")
    suspend fun getTotalPresentOverall(): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND (type = 'CLASS' OR type = 'MANUAL') AND type != 'CANCELLED'")
    suspend fun getTotalClassesForSubject(subjectId: Long): Int

    @Query("SELECT COUNT(*) FROM attendance_records WHERE subjectId = :subjectId AND isPresent = 1 AND (type = 'CLASS' OR type = 'MANUAL') AND type != 'CANCELLED'")
    suspend fun getPresentClassesForSubject(subjectId: Long): Int

    @Query("""
        SELECT 
            s.*,
            (SELECT COUNT(*) FROM attendance_records WHERE subjectId = s.id AND (type = 'CLASS' OR type = 'MANUAL') AND type != 'CANCELLED') as totalClasses,
            (SELECT COUNT(*) FROM attendance_records WHERE subjectId = s.id AND isPresent = 1 AND (type = 'CLASS' OR type = 'MANUAL') AND type != 'CANCELLED') as presentClasses
        FROM subjects s
    """)
    fun getSubjectsWithAttendance(): Flow<List<SubjectWithAttendance>>
}
