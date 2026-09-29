package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AttendanceRepository
import com.example.util.DeviceSecurityHelper
import com.example.util.GeolocationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: UserEntity) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class CheckInStep {
    object GeoRangeCheck : CheckInStep()
    object BiometricVerification : CheckInStep()
    object CodeOrBarcodeEntry : CheckInStep()
    data class Completed(val record: AttendanceRecordEntity) : CheckInStep()
}

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository
    val deviceId: String = DeviceSecurityHelper.getUniqueDeviceId(application)
    val deviceModel: String = DeviceSecurityHelper.getDeviceModelName()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AttendanceRepository(db.appDao())
        viewModelScope.launch {
            repository.seedSampleDataIfNeeded(deviceId)
        }
    }

    // --- Auth State ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // --- Courses & Sessions ---
    val allCourses: StateFlow<List<CourseEntity>> = repository.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSessions: StateFlow<List<ClassSessionEntity>> = repository.getActiveSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = repository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Student Specific States ---
    private val _enrolledCourses = MutableStateFlow<List<CourseEntity>>(emptyList())
    val enrolledCourses: StateFlow<List<CourseEntity>> = _enrolledCourses.asStateFlow()

    private val _studentAttendance = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val studentAttendance: StateFlow<List<AttendanceRecordEntity>> = _studentAttendance.asStateFlow()

    // Check-in flow state for student
    private val _selectedSessionForCheckIn = MutableStateFlow<ClassSessionEntity?>(null)
    val selectedSessionForCheckIn: StateFlow<ClassSessionEntity?> = _selectedSessionForCheckIn.asStateFlow()

    private val _currentCheckInStep = MutableStateFlow<CheckInStep>(CheckInStep.GeoRangeCheck)
    val currentCheckInStep: StateFlow<CheckInStep> = _currentCheckInStep.asStateFlow()

    // Simulated / GPS student location
    private val _studentLatitude = MutableStateFlow(6.5244)
    val studentLatitude: StateFlow<Double> = _studentLatitude.asStateFlow()

    private val _studentLongitude = MutableStateFlow(3.3792)
    val studentLongitude: StateFlow<Double> = _studentLongitude.asStateFlow()

    // Student distance offset in meters (for testing in emulator / indoors)
    private val _simulatedDistanceMeters = MutableStateFlow(1.2) // default within 2m
    val simulatedDistanceMeters: StateFlow<Double> = _simulatedDistanceMeters.asStateFlow()

    private val _isFaceVerified = MutableStateFlow(false)
    val isFaceVerified: StateFlow<Boolean> = _isFaceVerified.asStateFlow()

    private val _isFaceScanning = MutableStateFlow(false)
    val isFaceScanning: StateFlow<Boolean> = _isFaceScanning.asStateFlow()

    private val _checkInError = MutableStateFlow<String?>(null)
    val checkInError: StateFlow<String?> = _checkInError.asStateFlow()

    // --- Lecturer Specific States ---
    private val _selectedSessionAttendance = MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())
    val selectedSessionAttendance: StateFlow<List<AttendanceRecordEntity>> = _selectedSessionAttendance.asStateFlow()

    private val _lastCreatedSession = MutableStateFlow<ClassSessionEntity?>(null)
    val lastCreatedSession: StateFlow<ClassSessionEntity?> = _lastCreatedSession.asStateFlow()

    // --- Auth Actions ---

    fun registerLecturer(fullName: String, email: String, faculty: String, passcode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.registerLecturer(fullName, email, faculty, passcode)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { err ->
                    _authState.value = AuthState.Error(err.message ?: "Registration failed")
                }
            )
        }
    }

    fun registerStudent(fullName: String, matricNo: String, faculty: String, passcode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.registerStudent(fullName, matricNo, faculty, passcode, deviceId)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                    loadStudentData(user.id)
                },
                onFailure = { err ->
                    _authState.value = AuthState.Error(err.message ?: "Registration failed")
                }
            )
        }
    }

    fun loginLecturer(email: String, passcode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.loginLecturer(email, passcode)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                },
                onFailure = { err ->
                    _authState.value = AuthState.Error(err.message ?: "Login failed")
                }
            )
        }
    }

    fun loginStudent(matricNo: String, passcode: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.loginStudent(matricNo, passcode, deviceId)
            result.fold(
                onSuccess = { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(user)
                    loadStudentData(user.id)
                },
                onFailure = { err ->
                    _authState.value = AuthState.Error(err.message ?: "Login failed")
                }
            )
        }
    }

    fun logout() {
        _currentUser.value = null
        _authState.value = AuthState.Idle
        _selectedSessionForCheckIn.value = null
        _currentCheckInStep.value = CheckInStep.GeoRangeCheck
        _isFaceVerified.value = false
    }

    private fun loadStudentData(studentId: Long) {
        viewModelScope.launch {
            repository.getStudentEnrollments(studentId).collect { enrollments ->
                val enrolledIds = enrollments.map { it.courseId }.toSet()
                val courses = repository.getAllCourses().firstOrNull() ?: emptyList()
                _enrolledCourses.value = courses.filter { it.id in enrolledIds }
            }
        }
        viewModelScope.launch {
            repository.getStudentAttendance(studentId).collect {
                _studentAttendance.value = it
            }
        }
    }

    // --- Lecturer Actions ---

    fun createCourse(
        code: String,
        title: String,
        faculty: String,
        defaultRoom: String,
        credits: Int,
        onSuccess: (CourseEntity) -> Unit
    ) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val course = repository.createCourse(
                code = code,
                title = title,
                faculty = faculty,
                lecturerId = user.id,
                lecturerName = user.fullName,
                defaultRoom = defaultRoom,
                credits = credits
            )
            onSuccess(course)
        }
    }

    fun startClassSession(
        course: CourseEntity,
        roomName: String,
        sessionCode: String,
        durationMinutes: Int = 45,
        onCreated: (ClassSessionEntity) -> Unit
    ) {
        viewModelScope.launch {
            // Pin lecturer location
            val loc = GeolocationHelper.getDeviceLocation(getApplication())
            val lat = loc?.first ?: 6.5244
            val lon = loc?.second ?: 3.3792

            val session = repository.startClassSession(
                course = course,
                latitude = lat,
                longitude = lon,
                roomName = roomName,
                sessionCode = sessionCode,
                durationMinutes = durationMinutes
            )
            _lastCreatedSession.value = session
            loadSessionAttendance(session.id)
            onCreated(session)
        }
    }

    fun endClassSession(sessionId: Long) {
        viewModelScope.launch {
            repository.endSession(sessionId)
            if (_lastCreatedSession.value?.id == sessionId) {
                _lastCreatedSession.value = _lastCreatedSession.value?.copy(isActive = false)
            }
        }
    }

    fun loadSessionAttendance(sessionId: Long) {
        viewModelScope.launch {
            repository.getSessionAttendance(sessionId).collect {
                _selectedSessionAttendance.value = it
            }
        }
    }

    // --- Student Actions ---

    fun enrollCourse(courseId: Long) {
        val student = _currentUser.value ?: return
        viewModelScope.launch {
            repository.enrollInCourse(student.id, courseId)
            loadStudentData(student.id)
        }
    }

    fun unenrollCourse(courseId: Long) {
        val student = _currentUser.value ?: return
        viewModelScope.launch {
            repository.unenrollCourse(student.id, courseId)
            loadStudentData(student.id)
        }
    }

    fun prepareCheckIn(session: ClassSessionEntity) {
        _selectedSessionForCheckIn.value = session
        _currentCheckInStep.value = CheckInStep.GeoRangeCheck
        _isFaceVerified.value = false
        _isFaceScanning.value = false
        _checkInError.value = null

        // Align student coordinates with session + simulated distance offset
        updateStudentPositionWithDistance(_simulatedDistanceMeters.value, session)
    }

    fun setSimulatedDistance(meters: Double) {
        _simulatedDistanceMeters.value = meters
        val session = _selectedSessionForCheckIn.value
        if (session != null) {
            updateStudentPositionWithDistance(meters, session)
        }
    }

    private fun updateStudentPositionWithDistance(meters: Double, session: ClassSessionEntity) {
        // 1 degree latitude ~ 111,000 meters
        val deltaLat = meters / 111000.0
        _studentLatitude.value = session.latitude + deltaLat
        _studentLongitude.value = session.longitude
    }

    fun startFaceScan() {
        _isFaceScanning.value = true
    }

    fun onFaceScanSuccess() {
        _isFaceScanning.value = false
        _isFaceVerified.value = true
        _currentCheckInStep.value = CheckInStep.CodeOrBarcodeEntry
    }

    fun submitCheckIn(codeOrBarcode: String, method: String) {
        val student = _currentUser.value ?: return
        val session = _selectedSessionForCheckIn.value ?: return

        viewModelScope.launch {
            _checkInError.value = null
            val result = repository.submitAttendance(
                sessionId = session.id,
                student = student,
                studentLat = _studentLatitude.value,
                studentLon = _studentLongitude.value,
                inputCodeOrBarcode = codeOrBarcode,
                verificationMethod = method
            )

            result.fold(
                onSuccess = { record ->
                    _currentCheckInStep.value = CheckInStep.Completed(record)
                    loadStudentData(student.id)
                },
                onFailure = { err ->
                    _checkInError.value = err.message ?: "Attendance verification failed."
                }
            )
        }
    }

    fun dismissCheckIn() {
        _selectedSessionForCheckIn.value = null
        _currentCheckInStep.value = CheckInStep.GeoRangeCheck
        _isFaceVerified.value = false
        _checkInError.value = null
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }
}
