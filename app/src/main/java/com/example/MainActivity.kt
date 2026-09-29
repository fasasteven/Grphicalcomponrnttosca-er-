package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.auth.AuthScreen
import com.example.ui.lecturer.LecturerDashboardScreen
import com.example.ui.student.StudentDashboardScreen
import com.example.ui.theme.GeoAttendTheme
import com.example.viewmodel.AttendanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AttendanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GeoAttendTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    GeoAttendApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun GeoAttendApp(viewModel: AttendanceViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    var showWebAndApiInfo by remember { mutableStateOf(false) }

    BackHandler(enabled = currentUser != null) {
        viewModel.logout()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentUser,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "auth_navigation"
        ) { user ->
            if (user == null) {
                AuthScreen(viewModel = viewModel)
            } else {
                when (user.role) {
                    UserRole.LECTURER.name -> {
                        LecturerDashboardScreen(viewModel = viewModel)
                    }
                    UserRole.STUDENT.name -> {
                        StudentDashboardScreen(viewModel = viewModel)
                    }
                    else -> {
                        AuthScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Floating quick action to inspect Web App & Backend files
        SmallFloatingActionButton(
            onClick = { showWebAndApiInfo = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "Web & Backend Code",
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showWebAndApiInfo) {
        AlertDialog(
            onDismissRequest = { showWebAndApiInfo = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.IntegrationInstructions,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "JavaScript Web & API Files",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "All requested files are packaged and available in this project:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "1. Frontend Web App (Standalone)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "File: /frontend/index.html",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Self-contained HTML5/CSS3/JavaScript web app with 2m radar pulse canvas, facial biometric scanner, dynamic barcodes, 1-phone device lock, and scroll animations. Can be opened directly in any web browser or deployed to Netlify / Vercel.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "2. Backend Server & API",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF0F766E)
                            )
                            Text(
                                text = "Files: /backend/server.js\n/backend/package.json\n/backend/README.md",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Complete Node.js/Express server implementing Haversine 2-meter proximity calculation, passcode auth, course creation, active class broadcasts, and attendance logs. Run with 'npm install && npm start'.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "3. Native Android App (Running here)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = "100% Native Jetpack Compose UI with Room Database persistence, real-time animated radar, biometric scanner, and hardware device lock.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showWebAndApiInfo = false }) {
                    Text("Got It")
                }
            }
        )
    }
}
