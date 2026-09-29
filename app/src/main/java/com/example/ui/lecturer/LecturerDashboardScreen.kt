package com.example.ui.lecturer

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AVAILABLE_FACULTIES
import com.example.data.model.ClassSessionEntity
import com.example.data.model.CourseEntity
import com.example.util.BarcodeHelper
import com.example.util.BarcodeView
import com.example.util.GeolocationHelper
import com.example.util.QrMatrixView
import com.example.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturerDashboardScreen(viewModel: AttendanceViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allCourses by viewModel.allCourses.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val sessionAttendance by viewModel.selectedSessionAttendance.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Active Class, 1: My Courses, 2: Start Session
    var showCreateCourseDialog by remember { mutableStateOf(false) }
    var selectedSessionForRoster by remember { mutableStateOf<ClassSessionEntity?>(null) }

    // Update selected session to newest active session if available
    LaunchedEffect(activeSessions) {
        val myActive = activeSessions.firstOrNull { it.lecturerId == currentUser?.id }
        if (myActive != null && selectedSessionForRoster == null) {
            selectedSessionForRoster = myActive
            viewModel.loadSessionAttendance(myActive.id)
        }
    }

    val lecturerCourses = remember(allCourses, currentUser) {
        allCourses.filter { it.lecturerId == currentUser?.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Lecturer Portal",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = currentUser?.fullName ?: "Lecturer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Staff Passcode: ${currentUser?.passcode ?: "----"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("lecturer_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Log Out")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showCreateCourseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_create_course")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Course")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Faculty Header Card
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Faculty",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = currentUser?.faculty ?: "Faculty",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Navigation Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Active Class") },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Courses (${lecturerCourses.size})") },
                    icon = { Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Start Class") },
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> ActiveClassTab(
                    activeSessions = activeSessions.filter { it.lecturerId == currentUser?.id },
                    selectedSession = selectedSessionForRoster,
                    attendees = sessionAttendance,
                    onSelectSession = {
                        selectedSessionForRoster = it
                        viewModel.loadSessionAttendance(it.id)
                    },
                    onEndSession = { sessionId ->
                        viewModel.endClassSession(sessionId)
                        if (selectedSessionForRoster?.id == sessionId) {
                            selectedSessionForRoster = null
                        }
                    },
                    onGoToStartClass = { selectedTab = 2 }
                )
                1 -> MyCoursesTab(
                    courses = lecturerCourses,
                    onCreateCourseClick = { showCreateCourseDialog = true },
                    onStartSessionClick = { selectedTab = 2 }
                )
                2 -> StartSessionTab(
                    courses = lecturerCourses,
                    onStart = { course, room, code, duration ->
                        viewModel.startClassSession(course, room, code, duration) { session ->
                            selectedSessionForRoster = session
                            selectedTab = 0
                        }
                    },
                    onCreateCourseClick = { showCreateCourseDialog = true }
                )
            }
        }
    }

    if (showCreateCourseDialog) {
        CreateCourseDialog(
            defaultFaculty = currentUser?.faculty ?: AVAILABLE_FACULTIES.first(),
            onDismiss = { showCreateCourseDialog = false },
            onConfirm = { code, title, faculty, room, credits ->
                viewModel.createCourse(code, title, faculty, room, credits) {
                    showCreateCourseDialog = false
                    selectedTab = 1
                }
            }
        )
    }
}

@Composable
fun ActiveClassTab(
    activeSessions: List<ClassSessionEntity>,
    selectedSession: ClassSessionEntity?,
    attendees: List<com.example.data.model.AttendanceRecordEntity>,
    onSelectSession: (ClassSessionEntity) -> Unit,
    onEndSession: (Long) -> Unit,
    onGoToStartClass: () -> Unit
) {
    if (activeSessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = "No Active Session",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Class Currently In Session",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start a class session to project the 2m geofence beacon, attendance code, and barcode for students.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onGoToStartClass,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_empty_start_class")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start a Class Now")
                }
            }
        }
    } else {
        val currentSession = selectedSession ?: activeSessions.first()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Session Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE SESSION",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            TextButton(
                                onClick = { onEndSession(currentSession.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                                modifier = Modifier.testTag("btn_end_class_session")
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("End Class", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${currentSession.courseCode}: ${currentSession.courseTitle}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Location: ${currentSession.roomName} • Strict 2m Geofence Active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Display of 6-digit Code for Students
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CLASS ATTENDANCE CODE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentSession.sessionCode,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Share this 6-digit code or project barcode below in class",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Barcode & QR Code Section
                        Text(
                            text = "Barcode / QR Display for Students to Scan:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                BarcodeView(
                                    data = currentSession.sessionCode,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Column(
                                modifier = Modifier.size(110.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                QrMatrixView(
                                    data = "GEOATTEND:${currentSession.id}:${currentSession.sessionCode}",
                                    modifier = Modifier.size(110.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Real-Time Attendance Roster Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified Attendees (${attendees.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "2m Range & Biometrics Verified",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (attendees.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Awaiting student clock-ins...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Students within 2m with biometric face match and code will appear here in real time.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(attendees) { record ->
                    AttendeeCard(record = record)
                }
            }
        }
    }
}

@Composable
fun AttendeeCard(record: com.example.data.model.AttendanceRecordEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Present",
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.studentName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Matric: ${record.studentMatric} • Dist: ${GeolocationHelper.formatDistance(record.distanceMeters)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Auth: ${record.verificationMethod.replace('_', ' ')}",
                    fontSize = 10.sp,
                    color = Color(0xFF0D9488),
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MyCoursesTab(
    courses: List<CourseEntity>,
    onCreateCourseClick: () -> Unit,
    onStartSessionClick: () -> Unit
) {
    if (courses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Courses Created Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You must create a course before students can register for it and take attendance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onCreateCourseClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_create_first_course")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Course")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Courses You Offer",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedButton(
                        onClick = onCreateCourseClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_add_another_course")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Course", fontSize = 12.sp)
                    }
                }
            }

            items(courses) { course ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = course.code,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "${course.credits} Credits • ${course.semester}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Hall: ${course.defaultRoom} • ${course.faculty}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onStartSessionClick,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Class Session for this Course", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartSessionTab(
    courses: List<CourseEntity>,
    onStart: (CourseEntity, String, String, Int) -> Unit,
    onCreateCourseClick: () -> Unit
) {
    var selectedCourse by remember { mutableStateOf<CourseEntity?>(courses.firstOrNull()) }
    var roomName by remember { mutableStateOf(courses.firstOrNull()?.defaultRoom ?: "Lecture Hall 1") }
    var generatedCode by remember { mutableStateOf(BarcodeHelper.generateSessionCode()) }
    var durationMinutes by remember { mutableStateOf(45) }

    LaunchedEffect(courses) {
        if (selectedCourse == null && courses.isNotEmpty()) {
            selectedCourse = courses.first()
            roomName = courses.first().defaultRoom
        }
    }

    if (courses.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Please create a course first before setting a class.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onCreateCourseClick) {
                    Text("Create Course")
                }
            }
        }
        return
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Geolocation Geofence Ready",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Starting class pins your current GPS coordinates. Students must be within 2.0 meters to clock in.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "1. Select Course to Conduct",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        courses.forEach { course ->
            val isSelected = selectedCourse?.id == course.id
            Card(
                onClick = {
                    selectedCourse = course
                    roomName = course.defaultRoom
                },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            selectedCourse = course
                            roomName = course.defaultRoom
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${course.code} - ${course.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Faculty: ${course.faculty}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text(
            text = "2. Classroom Location / Room",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        OutlinedTextField(
            value = roomName,
            onValueChange = { roomName = it },
            label = { Text("Room / Hall / Lab Name") },
            leadingIcon = { Icon(Icons.Default.Room, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text(
            text = "3. Dynamic Attendance Code",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = generatedCode,
                onValueChange = { generatedCode = it },
                label = { Text("6-Digit Code") },
                leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            IconButton(
                onClick = { generatedCode = BarcodeHelper.generateSessionCode() }
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Regenerate Code")
            }
        }

        // Preview Barcode for this code
        BarcodeView(
            data = generatedCode,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                val course = selectedCourse
                if (course != null) {
                    onStart(course, roomName, generatedCode, durationMinutes)
                }
            },
            enabled = selectedCourse != null && roomName.isNotBlank() && generatedCode.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_launch_class_session"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.BroadcastOnPersonal, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch Class & Broadcast Notification", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCourseDialog(
    defaultFaculty: String,
    onDismiss: () -> Unit,
    onConfirm: (code: String, title: String, faculty: String, room: String, credits: Int) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var faculty by remember { mutableStateOf(defaultFaculty) }
    var room by remember { mutableStateOf("Lecture Hall A") }
    var credits by remember { mutableStateOf("3") }
    var facultyExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create New Course",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. CS301)") },
                    modifier = Modifier.fillMaxWidth().testTag("input_course_code"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Course Title") },
                    placeholder = { Text("Mobile Systems Engineering") },
                    modifier = Modifier.fillMaxWidth().testTag("input_course_title"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                ExposedDropdownMenuBox(
                    expanded = facultyExpanded,
                    onExpandedChange = { facultyExpanded = !facultyExpanded }
                ) {
                    OutlinedTextField(
                        value = faculty,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Faculty") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = facultyExpanded,
                        onDismissRequest = { facultyExpanded = false }
                    ) {
                        AVAILABLE_FACULTIES.forEach { fac ->
                            DropdownMenuItem(
                                text = { Text(fac, fontSize = 12.sp) },
                                onClick = {
                                    faculty = fac
                                    facultyExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Default Hall") },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = credits,
                        onValueChange = { credits = it },
                        label = { Text("Credits") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(code, title, faculty, room, credits.toIntOrNull() ?: 3)
                },
                enabled = code.isNotBlank() && title.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_create_course")
            ) {
                Text("Create Course")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
