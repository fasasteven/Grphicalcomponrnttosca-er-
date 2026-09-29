package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Users
    @Query("SELECT * FROM users WHERE identifier = :identifier LIMIT 1")
    suspend fun getUserByIdentifier(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    // Courses
    @Query("SELECT * FROM courses ORDER BY code ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE faculty = :faculty ORDER BY code ASC")
    fun getCoursesByFaculty(faculty: String): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE lecturerId = :lecturerId ORDER BY id DESC")
    fun getCoursesByLecturer(lecturerId: Long): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :courseId LIMIT 1")
    suspend fun getCourseById(courseId: Long): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Delete
    suspend fun deleteCourse(course: CourseEntity)

    // Enrollments
    @Query("SELECT * FROM enrollments WHERE studentId = :studentId")
    fun getEnrollmentsForStudent(studentId: Long): Flow<List<EnrollmentEntity>>

    @Query("SELECT * FROM enrollments WHERE studentId = :studentId AND courseId = :courseId LIMIT 1")
    suspend fun getEnrollment(studentId: Long, courseId: Long): EnrollmentEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEnrollment(enrollment: EnrollmentEntity): Long

    @Query("DELETE FROM enrollments WHERE studentId = :studentId AND courseId = :courseId")
    suspend fun deleteEnrollment(studentId: Long, courseId: Long)

    // Sessions
    @Query("SELECT * FROM class_sessions WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveSessions(): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE lecturerId = :lecturerId ORDER BY createdAt DESC")
    fun getSessionsByLecturer(lecturerId: Long): Flow<List<ClassSessionEntity>>

    @Query("SELECT * FROM class_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): ClassSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ClassSessionEntity): Long

    @Query("UPDATE class_sessions SET isActive = 0 WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long)

    // Attendance
    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getAttendanceForSession(sessionId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE sessionId = :sessionId AND studentId = :studentId LIMIT 1")
    suspend fun getStudentAttendanceForSession(sessionId: Long, studentId: Long): AttendanceRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecordEntity): Long

    @Query("SELECT COUNT(*) FROM attendance_records WHERE courseId = :courseId AND studentId = :studentId")
    fun getStudentAttendanceCountForCourse(courseId: Long, studentId: Long): Flow<Int>

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    // Device Bindings (Strictly 1 Student per Phone)
    @Query("SELECT * FROM device_bindings WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceBinding(deviceId: String): DeviceBindingEntity?

    @Query("SELECT * FROM device_bindings WHERE boundStudentId = :studentId LIMIT 1")
    suspend fun getBindingForStudent(studentId: Long): DeviceBindingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceBinding(binding: DeviceBindingEntity)

    @Query("DELETE FROM device_bindings WHERE deviceId = :deviceId")
    suspend fun clearDeviceBinding(deviceId: String)
}
