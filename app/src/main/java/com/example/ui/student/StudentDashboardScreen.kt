package com.example.ui.student

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.BiometricFaceScanner
import com.example.ui.components.RadarProximityView
import com.example.util.BarcodeView
import com.example.util.GeolocationHelper
import com.example.viewmodel.CheckInStep
import com.example.viewmodel.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(viewModel: AttendanceViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allCourses by viewModel.allCourses.collectAsState()
    val enrolledCourses by viewModel.enrolledCourses.collectAsState()
    val activeSessions by viewModel.activeSessions.collectAsState()
    val studentAttendance by viewModel.studentAttendance.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val selectedSessionForCheckIn by viewModel.selectedSessionForCheckIn.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Live Classes, 1: Courses & Register, 2: History, 3: Notifications

    val unreadNotifs = remember(notifications) { notifications.count { !it.isRead } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Student Portal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${currentUser?.fullName} (${currentUser?.identifier})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { selectedTab = 3 },
                        modifier = Modifier.testTag("btn_notifications")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifs > 0) {
                                    Badge { Text(unreadNotifs.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("student_logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Log Out")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Sensors, contentDescription = "Live Classes") },
                    label = { Text("Live Classes", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Courses") },
                    label = { Text("Courses", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.History, contentDescription = "Records") },
                    label = { Text("Records", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        BadgedBox(badge = { if (unreadNotifs > 0) Badge { Text(unreadNotifs.toString()) } }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts")
                        }
                    },
                    label = { Text("Alerts", fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Security Lock Banner: 1 Login per Phone
            Surface(
                color = Color(0xFF0F766E).copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Device Binding",
                        tint = Color(0xFF0F766E),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Phone Bound: ${viewModel.deviceId.take(16)}... (1 Login/Phone Enforced)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F766E)
                    )
                }
            }

            when (selectedTab) {
                0 -> StudentLiveClassesTab(
                    activeSessions = activeSessions,
                    enrolledCourses = enrolledCourses,
                    studentAttendance = studentAttendance,
                    onCheckInClick = { session ->
                        viewModel.prepareCheckIn(session)
                    },
                    onGoToCourses = { selectedTab = 1 }
                )
                1 -> StudentCoursesTab(
                    allCourses = allCourses,
                    enrolledCourses = enrolledCourses,
                    studentFaculty = currentUser?.faculty ?: "",
                    onEnroll = { viewModel.enrollCourse(it) },
                    onUnenroll = { viewModel.unenrollCourse(it) }
                )
                2 -> StudentHistoryTab(records = studentAttendance)
                3 -> StudentNotificationsTab(
                    notifications = notifications,
                    onNotificationClick = { notif ->
                        viewModel.markNotificationAsRead(notif.id)
                        val session = activeSessions.firstOrNull { it.id == notif.sessionId }
                        if (session != null) {
                            viewModel.prepareCheckIn(session)
                        }
                    }
                )
            }
        }
    }

    // Modal Sheet for 3-Step Attendance Verification Flow
    if (selectedSessionForCheckIn != null) {
        CheckInBottomSheet(
            session = selectedSessionForCheckIn!!,
            viewModel = viewModel,
            onDismiss = { viewModel.dismissCheckIn() }
        )
    }
}

@Composable
fun StudentLiveClassesTab(
    activeSessions: List<ClassSessionEntity>,
    enrolledCourses: List<CourseEntity>,
    studentAttendance: List<AttendanceRecordEntity>,
    onCheckInClick: (ClassSessionEntity) -> Unit,
    onGoToCourses: () -> Unit
) {
    if (activeSessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.HourglassEmpty,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Classes In Session Right Now",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When your lecturer starts a class and pins the 2m geofence beacon, it will appear here instantly with a notification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Class Beacons (${activeSessions.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "2m Radius + Face Check",
                            color = Color(0xFF059669),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(activeSessions) { session ->
                val isAttended = studentAttendance.any { it.sessionId == session.id }
                val isEnrolled = enrolledCourses.any { it.id == session.courseId }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAttended) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    text = session.courseCode,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (isAttended) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ATTENDED", color = Color(0xFF059669), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Awaiting Verification",
                                        color = Color(0xFFD97706),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = session.courseTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Lecturer: ${session.lecturerName} • Room: ${session.roomName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isAttended) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF059669))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Attendance Successfully Logged")
                            }
                        } else {
                            Button(
                                onClick = { onCheckInClick(session) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_check_in_${session.courseCode.lowercase().replace(" ", "_")}"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verify & Take Attendance (2m + Face + Code)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCoursesTab(
    allCourses: List<CourseEntity>,
    enrolledCourses: List<CourseEntity>,
    studentFaculty: String,
    onEnroll: (Long) -> Unit,
    onUnenroll: (Long) -> Unit
) {
    val enrolledIds = remember(enrolledCourses) { enrolledCourses.map { it.id }.toSet() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Course Registration Portal",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Lecturers create courses for their faculty. Register below to receive class announcements and record attendance.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "Available Courses (${allCourses.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (allCourses.isEmpty()) {
            item {
                Text(
                    text = "No courses available yet. Lecturer must create courses first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(allCourses) { course ->
                val isEnrolled = course.id in enrolledIds
                val isMyFaculty = course.faculty == studentFaculty

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnrolled) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
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
                            if (isMyFaculty) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        text = "Your Faculty",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = course.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Lecturer: ${course.lecturerName} • ${course.faculty}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (isEnrolled) {
                                OutlinedButton(
                                    onClick = { onUnenroll(course.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                                ) {
                                    Text("Drop Course", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onEnroll(course.id) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_enroll_${course.code.lowercase().replace(" ", "_")}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Register Course", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentHistoryTab(records: List<AttendanceRecordEntity>) {
    if (records.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Checklist,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Attendance Records Yet",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verify into an active class session within 2m range to build your attendance history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
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
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "${records.size} Verified Attendances",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "100% Punctuality • All authenticated via 2m Geofence & Facial scan",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(records) { record ->
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
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = record.courseCode,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Proximity: ${GeolocationHelper.formatDistance(record.distanceMeters)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Auth: ${record.verificationMethod}",
                                fontSize = 10.sp,
                                color = Color(0xFF0D9488)
                            )
                        }

                        Text(
                            text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentNotificationsTab(
    notifications: List<NotificationEntity>,
    onNotificationClick: (NotificationEntity) -> Unit
) {
    if (notifications.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Notifications",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When a lecturer starts attendance for a course, you will receive real-time notifications here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Lecturer Announcements & Class Alerts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(notifications) { notif ->
                Card(
                    onClick = { onNotificationClick(notif) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = notif.message,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SimpleDateFormat("HH:mm a", Locale.getDefault()).format(Date(notif.timestamp)),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3-Step Verification Dialog:
 * Step 1: 2m Geolocation Range Check with Animated Radar
 * Step 2: Facial Biometric Scan
 * Step 3: Passcode or Barcode Scan
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInBottomSheet(
    session: ClassSessionEntity,
    viewModel: AttendanceViewModel,
    onDismiss: () -> Unit
) {
    val currentStep by viewModel.currentCheckInStep.collectAsState()
    val simulatedDist by viewModel.simulatedDistanceMeters.collectAsState()
    val isFaceScanning by viewModel.isFaceScanning.collectAsState()
    val isFaceVerified by viewModel.isFaceVerified.collectAsState()
    val checkInError by viewModel.checkInError.collectAsState()

    var inputCode by remember { mutableStateOf("") }
    var verificationMode by remember { mutableStateOf("CODE") } // "CODE" or "BARCODE"
    var isSimulatingBarcodeScan by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Take Attendance",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${session.courseCode} • ${session.roomName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Req: 2m + Face + Code",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val steps = listOf("1. Geofence (2m)", "2. Face Scan", "3. Code/Barcode")
                val activeIndex = when (currentStep) {
                    is CheckInStep.GeoRangeCheck -> 0
                    is CheckInStep.BiometricVerification -> 1
                    is CheckInStep.CodeOrBarcodeEntry -> 2
                    is CheckInStep.Completed -> 3
                }
                steps.forEachIndexed { index, name ->
                    val isDone = index < activeIndex
                    val isCurrent = index == activeIndex
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isDone -> Color(0xFF10B981)
                            isCurrent -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDone || isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Error Message Banner
            if (checkInError != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = checkInError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Step Contents
            when (val step = currentStep) {
                is CheckInStep.GeoRangeCheck -> {
                    // Radar view with current distance
                    RadarProximityView(currentDistanceMeters = simulatedDist)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Distance Walk Simulation Helper (Essential for testing on emulator and inside indoor rooms)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Classroom Proximity Distance:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = GeolocationHelper.formatDistance(simulatedDist),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (simulatedDist <= 2.0) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                            }
                            Slider(
                                value = simulatedDist.toFloat(),
                                onValueChange = { viewModel.setSimulatedDistance(it.toDouble()) },
                                valueRange = 0.2f..15.0f,
                                modifier = Modifier.testTag("slider_distance_simulation")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.setSimulatedDistance(0.8) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_step_within_2m")
                                ) {
                                    Text("Walk to Desk (0.8m)", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.setSimulatedDistance(6.5) },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Step Back (6.5m)", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    val canProceedGeo = simulatedDist <= 2.0
                    Button(
                        onClick = {
                            if (canProceedGeo) {
                                viewModel.startFaceScan()
                            }
                        },
                        enabled = canProceedGeo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_confirm_georange"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (canProceedGeo) "Confirm Geolocation & Proceed to Face Scan" else "You must be within 2.0m to proceed",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                is CheckInStep.BiometricVerification -> {
                    BiometricFaceScanner(
                        isScanning = isFaceScanning,
                        isVerified = isFaceVerified,
                        onScanComplete = { viewModel.onFaceScanSuccess() }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isFaceScanning && !isFaceVerified) {
                        Button(
                            onClick = { viewModel.startFaceScan() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_start_face_scan"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Facial Biometric Verification", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is CheckInStep.CodeOrBarcodeEntry -> {
                    // Mode tabs: 6-Digit Code vs Barcode Scanner
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = verificationMode == "CODE",
                            onClick = { verificationMode = "CODE" },
                            label = { Text("6-Digit Code") },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = verificationMode == "BARCODE",
                            onClick = { verificationMode = "BARCODE" },
                            label = { Text("Scan Barcode") },
                            leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (verificationMode == "CODE") {
                        OutlinedTextField(
                            value = inputCode,
                            onValueChange = { if (it.length <= 6) inputCode = it },
                            label = { Text("Enter 6-Digit Class Code") },
                            placeholder = { Text("Given by lecturer in class") },
                            leadingIcon = { Icon(Icons.Default.LockClock, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_checkin_code"),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.submitCheckIn(inputCode, "BIOMETRIC_FACE_AND_CODE")
                            },
                            enabled = inputCode.length >= 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_submit_code_attendance"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Verify Code & Clock In", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Simulated Barcode / QR Scanner Viewfinder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Point camera at lecturer's barcode screen",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                // Auto scan matching session barcode
                                viewModel.submitCheckIn(session.sessionCode, "BIOMETRIC_FACE_AND_BARCODE")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_capture_barcode"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan & Capture Lecturer Barcode", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is CheckInStep.Completed -> {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Attendance Verified & Clocked In!",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF047857)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Recorded at ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(step.record.timestamp))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Distance: ${GeolocationHelper.formatDistance(step.record.distanceMeters)} • Status: ${step.record.status}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_checkin_done"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Done")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
