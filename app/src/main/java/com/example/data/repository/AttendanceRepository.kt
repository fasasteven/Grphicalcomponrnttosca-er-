package com.example.data.repository

import com.example.data.local.AppDao
import com.example.data.model.*
import com.example.util.GeolocationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class AttendanceRepository(private val dao: AppDao) {

    // --- Authentication ---

    suspend fun registerLecturer(
        fullName: String,
        email: String,
        faculty: String,
        passcode: String
    ): Result<UserEntity> {
        val existing = dao.getUserByIdentifier(email.trim().lowercase())
        if (existing != null) {
            return Result.failure(Exception("An account with this email/staff ID already exists."))
        }
        val newUser = UserEntity(
            role = UserRole.LECTURER.name,
            fullName = fullName.trim(),
            identifier = email.trim().lowercase(),
            faculty = faculty,
            passcode = passcode.trim()
        )
        val id = dao.insertUser(newUser)
        return Result.success(newUser.copy(id = id))
    }

    suspend fun registerStudent(
        fullName: String,
        matricNo: String,
        faculty: String,
        passcode: String,
        deviceId: String
    ): Result<UserEntity> {
        // Enforce 1 login per phone: check if this device is already bound to another student
        val existingBinding = dao.getDeviceBinding(deviceId)
        val cleanMatric = matricNo.trim().uppercase()

        if (existingBinding != null && existingBinding.boundStudentMatric != cleanMatric) {
            return Result.failure(
                Exception(
                    "Device Security Policy: This phone is already bound to student (${existingBinding.boundStudentMatric} - ${existingBinding.boundStudentName}). Multiple student logins per device are strictly prohibited."
                )
            )
        }

        val existingUser = dao.getUserByIdentifier(cleanMatric)
        if (existingUser != null) {
            return Result.failure(Exception("Student with matric no '$cleanMatric' is already registered."))
        }

        val newUser = UserEntity(
            role = UserRole.STUDENT.name,
            fullName = fullName.trim(),
            identifier = cleanMatric,
            faculty = faculty,
            passcode = passcode.trim(),
            boundDeviceId = deviceId
        )
        val id = dao.insertUser(newUser)

        // Bind device
        dao.insertDeviceBinding(
            DeviceBindingEntity(
                deviceId = deviceId,
                boundStudentId = id,
                boundStudentMatric = cleanMatric,
                boundStudentName = fullName.trim()
            )
        )

        return Result.success(newUser.copy(id = id))
    }

    suspend fun loginLecturer(email: String, passcode: String): Result<UserEntity> {
        val user = dao.getUserByIdentifier(email.trim().lowercase())
            ?: return Result.failure(Exception("Lecturer account not found with this email/staff ID."))

        if (user.role != UserRole.LECTURER.name) {
            return Result.failure(Exception("This account is registered as a Student, not a Lecturer."))
        }

        if (user.passcode != passcode.trim()) {
            return Result.failure(Exception("Invalid passcode. Please re-enter your secret passcode."))
        }

        return Result.success(user)
    }

    suspend fun loginStudent(matricNo: String, passcode: String, deviceId: String): Result<UserEntity> {
        val cleanMatric = matricNo.trim().uppercase()
        val user = dao.getUserByIdentifier(cleanMatric)
            ?: return Result.failure(Exception("Student account not found with matric number '$cleanMatric'."))

        if (user.role != UserRole.STUDENT.name) {
            return Result.failure(Exception("This account is registered as a Lecturer, not a Student."))
        }

        if (user.passcode != passcode.trim()) {
            return Result.failure(Exception("Invalid passcode. Please enter your secret passcode."))
        }

        // Check Device Binding Constraint: One login per phone
        val bindingOnThisPhone = dao.getDeviceBinding(deviceId)
        if (bindingOnThisPhone != null && bindingOnThisPhone.boundStudentMatric != cleanMatric) {
            return Result.failure(
                Exception(
                    "Device Binding Lock: This device belongs to another student (${bindingOnThisPhone.boundStudentMatric}). Only one student login per phone is allowed."
                )
            )
        }

        // Check if student was previously bound to another device
        val studentBinding = dao.getBindingForStudent(user.id)
        if (studentBinding != null && studentBinding.deviceId != deviceId) {
            return Result.failure(
                Exception(
                    "Account Lock: Your student account is bound to another phone (${studentBinding.deviceId.take(14)}...). You cannot log in from a different device."
                )
            )
        }

        // If no binding exists yet for this student, bind to this phone now
        if (studentBinding == null) {
            dao.insertDeviceBinding(
                DeviceBindingEntity(
                    deviceId = deviceId,
                    boundStudentId = user.id,
                    boundStudentMatric = cleanMatric,
                    boundStudentName = user.fullName
                )
            )
            dao.updateUser(user.copy(boundDeviceId = deviceId))
        }

        return Result.success(user)
    }

    // --- Courses ---

    fun getAllCourses(): Flow<List<CourseEntity>> = dao.getAllCourses()

    fun getCoursesByFaculty(faculty: String): Flow<List<CourseEntity>> = dao.getCoursesByFaculty(faculty)

    fun getCoursesByLecturer(lecturerId: Long): Flow<List<CourseEntity>> = dao.getCoursesByLecturer(lecturerId)

    suspend fun createCourse(
        code: String,
        title: String,
        faculty: String,
        lecturerId: Long,
        lecturerName: String,
        defaultRoom: String,
        credits: Int
    ): CourseEntity {
        val course = CourseEntity(
            code = code.trim().uppercase(),
            title = title.trim(),
            faculty = faculty,
            lecturerId = lecturerId,
            lecturerName = lecturerName,
            defaultRoom = defaultRoom.trim().ifEmpty { "Lecture Hall 1" },
            credits = credits
        )
        val id = dao.insertCourse(course)
        return course.copy(id = id)
    }

    // --- Enrollments ---

    fun getStudentEnrollments(studentId: Long): Flow<List<EnrollmentEntity>> =
        dao.getEnrollmentsForStudent(studentId)

    suspend fun enrollInCourse(studentId: Long, courseId: Long): Boolean {
        val existing = dao.getEnrollment(studentId, courseId)
        if (existing == null) {
            dao.insertEnrollment(EnrollmentEntity(studentId = studentId, courseId = courseId))
            return true
        }
        return false
    }

    suspend fun unenrollCourse(studentId: Long, courseId: Long) {
        dao.deleteEnrollment(studentId, courseId)
    }

    // --- Class Sessions ---

    fun getActiveSessions(): Flow<List<ClassSessionEntity>> = dao.getActiveSessions()

    fun getLecturerSessions(lecturerId: Long): Flow<List<ClassSessionEntity>> =
        dao.getSessionsByLecturer(lecturerId)

    suspend fun startClassSession(
        course: CourseEntity,
        latitude: Double,
        longitude: Double,
        roomName: String,
        sessionCode: String,
        durationMinutes: Int = 45
    ): ClassSessionEntity {
        val session = ClassSessionEntity(
            courseId = course.id,
            courseCode = course.code,
            courseTitle = course.title,
            lecturerId = course.lecturerId,
            lecturerName = course.lecturerName,
            latitude = latitude,
            longitude = longitude,
            roomName = roomName,
            sessionCode = sessionCode,
            maxRadiusMeters = 2.0, // Strictly 2m
            durationMinutes = durationMinutes,
            isActive = true
        )
        val sessionId = dao.insertSession(session)

        // Trigger notification for students
        dao.insertNotification(
            NotificationEntity(
                targetCourseId = course.id,
                courseCode = course.code,
                title = "New Class Session: ${course.code}",
                message = "${course.lecturerName} started attendance in $roomName. Enter within 2m range to clock in.",
                sessionId = sessionId
            )
        )

        return session.copy(id = sessionId)
    }

    suspend fun endSession(sessionId: Long) {
        dao.endSession(sessionId)
    }

    // --- Attendance ---

    fun getSessionAttendance(sessionId: Long): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForSession(sessionId)

    fun getStudentAttendance(studentId: Long): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForStudent(studentId)

    suspend fun submitAttendance(
        sessionId: Long,
        student: UserEntity,
        studentLat: Double,
        studentLon: Double,
        inputCodeOrBarcode: String,
        verificationMethod: String
    ): Result<AttendanceRecordEntity> {
        val session = dao.getSessionById(sessionId)
            ?: return Result.failure(Exception("Session not found or has expired."))

        if (!session.isActive) {
            return Result.failure(Exception("This class session has been closed by the lecturer."))
        }

        // Check if student already attended
        val existing = dao.getStudentAttendanceForSession(sessionId, student.id)
        if (existing != null) {
            return Result.failure(Exception("You have already recorded attendance for this session!"))
        }

        // 1. Geolocation Check: Distance must be within 2.0 meters
        val distance = GeolocationHelper.calculateDistanceMeters(
            studentLat,
            studentLon,
            session.latitude,
            session.longitude
        )

        if (distance > session.maxRadiusMeters) {
            return Result.failure(
                Exception(
                    "Out of Range! You are ${GeolocationHelper.formatDistance(distance)} away. " +
                            "You must be within 2.0 meters of lecturer's location at ${session.roomName}."
                )
            )
        }

        // 2. Code or Barcode verification
        val cleanInput = inputCodeOrBarcode.trim()
        val isCodeValid = cleanInput == session.sessionCode ||
                cleanInput.contains(session.sessionCode) ||
                cleanInput == "GEOATTEND-${session.sessionCode}"

        if (!isCodeValid) {
            return Result.failure(Exception("Invalid session code/barcode! Please check code with lecturer."))
        }

        // Record attendance
        val record = AttendanceRecordEntity(
            sessionId = session.id,
            courseId = session.courseId,
            courseCode = session.courseCode,
            studentId = student.id,
            studentName = student.fullName,
            studentMatric = student.identifier,
            distanceMeters = distance,
            verificationMethod = verificationMethod,
            status = "PRESENT"
        )
        val id = dao.insertAttendance(record)
        return Result.success(record.copy(id = id))
    }

    // --- Notifications ---

    fun getNotifications(): Flow<List<NotificationEntity>> = dao.getAllNotifications()

    suspend fun markNotificationAsRead(id: Long) = dao.markNotificationAsRead(id)

    // --- Initial Seed Helper ---

    suspend fun seedSampleDataIfNeeded(deviceId: String) {
        val courses = dao.getAllCourses().firstOrNull()
        if (courses.isNullOrEmpty()) {
            // Seed a lecturer
            val lecturer = UserEntity(
                role = UserRole.LECTURER.name,
                fullName = "Dr. Alan Turing",
                identifier = "alan.turing@university.edu",
                faculty = "Faculty of Computer Science & Informatics",
                passcode = "1234"
            )
            val lecturerId = dao.insertUser(lecturer)

            // Seed sample courses
            val c1 = CourseEntity(
                code = "CSC 301",
                title = "Mobile & Ubiquitous Computing",
                faculty = "Faculty of Computer Science & Informatics",
                lecturerId = lecturerId,
                lecturerName = "Dr. Alan Turing",
                defaultRoom = "Science Lab 304",
                credits = 3
            )
            val c2 = CourseEntity(
                code = "CSC 305",
                title = "Distributed Systems & Cloud Architecture",
                faculty = "Faculty of Computer Science & Informatics",
                lecturerId = lecturerId,
                lecturerName = "Dr. Alan Turing",
                defaultRoom = "Lecture Theater B",
                credits = 4
            )
            val c3 = CourseEntity(
                code = "ENG 201",
                title = "Applied Sensor & Embedded Systems",
                faculty = "Faculty of Engineering & Technology",
                lecturerId = lecturerId,
                lecturerName = "Dr. Alan Turing",
                defaultRoom = "Engineering Complex 12",
                credits = 3
            )
            val c1Id = dao.insertCourse(c1)
            dao.insertCourse(c2)
            dao.insertCourse(c3)

            // Create an active session with sample location
            val sessionCode = "749215"
            val sampleSession = ClassSessionEntity(
                courseId = c1Id,
                courseCode = "CSC 301",
                courseTitle = "Mobile & Ubiquitous Computing",
                lecturerId = lecturerId,
                lecturerName = "Dr. Alan Turing",
                latitude = 6.5244,
                longitude = 3.3792,
                roomName = "Science Lab 304",
                sessionCode = sessionCode,
                maxRadiusMeters = 2.0,
                durationMinutes = 45,
                isActive = true
            )
            val sessionId = dao.insertSession(sampleSession)

            // Notification
            dao.insertNotification(
                NotificationEntity(
                    targetCourseId = c1Id,
                    courseCode = "CSC 301",
                    title = "Class Active: CSC 301",
                    message = "Dr. Alan Turing started attendance in Science Lab 304. 2m radius check active.",
                    sessionId = sessionId
                )
            )
        }
    }
}
