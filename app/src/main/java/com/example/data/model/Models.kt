package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    LECTURER,
    STUDENT
}

val AVAILABLE_FACULTIES = listOf(
    "Faculty of Computer Science & Informatics",
    "Faculty of Engineering & Technology",
    "Faculty of Medical & Health Sciences",
    "Faculty of Business & Management",
    "Faculty of Natural & Applied Sciences",
    "Faculty of Arts & Humanities",
    "Faculty of Law & Social Sciences"
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val role: String, // "LECTURER" or "STUDENT"
    val fullName: String,
    val identifier: String, // email for lecturer, matric no for student
    val faculty: String,
    val passcode: String, // Passcode created when signing up
    val boundDeviceId: String = "", // Bound device UUID (enforces 1 login per phone for students)
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String, // e.g. "CS301"
    val title: String, // e.g. "Mobile Cloud Computing"
    val faculty: String,
    val lecturerId: Long,
    val lecturerName: String,
    val defaultRoom: String = "Hall A",
    val semester: String = "First Semester",
    val credits: Int = 3,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "enrollments")
data class EnrollmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val studentId: Long,
    val courseId: Long,
    val enrolledAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "class_sessions")
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long,
    val courseCode: String,
    val courseTitle: String,
    val lecturerId: Long,
    val lecturerName: String,
    val latitude: Double, // Pinned GPS latitude
    val longitude: Double, // Pinned GPS longitude
    val roomName: String,
    val sessionCode: String, // 6-digit code for class
    val maxRadiusMeters: Double = 2.0, // Strictly 2m range restriction
    val durationMinutes: Int = 45,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val courseId: Long,
    val courseCode: String,
    val studentId: Long,
    val studentName: String,
    val studentMatric: String,
    val distanceMeters: Double, // Recorded distance (must be <= 2.0m)
    val verificationMethod: String, // "BIOMETRIC_AND_CODE" or "BIOMETRIC_AND_BARCODE"
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "PRESENT" // "PRESENT", "FLAGGED"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetCourseId: Long,
    val courseCode: String,
    val title: String,
    val message: String,
    val sessionId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "device_bindings")
data class DeviceBindingEntity(
    @PrimaryKey
    val deviceId: String,
    val boundStudentId: Long,
    val boundStudentMatric: String,
    val boundStudentName: String,
    val boundAt: Long = System.currentTimeMillis()
)
