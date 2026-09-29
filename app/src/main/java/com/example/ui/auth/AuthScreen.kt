package com.example.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AVAILABLE_FACULTIES
import com.example.data.model.UserRole
import com.example.viewmodel.AuthState
import com.example.viewmodel.AttendanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AttendanceViewModel) {
    val authState by viewModel.authState.collectAsState()
    val scrollState = rememberScrollState()

    var isSignUp by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var fullName by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") } // email for lecturer, matric for student
    var passcode by remember { mutableStateOf("") }
    var passcodeVisible by remember { mutableStateOf(false) }
    var selectedFaculty by remember { mutableStateOf(AVAILABLE_FACULTIES.first()) }
    var facultyExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Header with Gradient Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1E3A8A), Color(0xFF0D9488))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GpsFixed,
                    contentDescription = "GeoAttend Logo",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GeoAttend",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Geofenced & Biometric Attendance System",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Role Selector Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val roles = listOf(UserRole.STUDENT to "Student", UserRole.LECTURER to "Lecturer")
                    roles.forEach { (role, title) ->
                        val isSelected = selectedRole == role
                        Surface(
                            onClick = { selectedRole = role },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_tab_${role.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (role == UserRole.STUDENT) Icons.Default.School else Icons.Default.Person,
                                    contentDescription = title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Mode Tabs (Sign In vs Sign Up)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUp) "Create Account" else "Welcome Back",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = {
                        isSignUp = !isSignUp
                        fullName = ""
                        passcode = ""
                    },
                    modifier = Modifier.testTag("toggle_signup_signin")
                ) {
                    Text(
                        text = if (isSignUp) "Already have account? Sign In" else "New here? Register",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // One phone per student notice
            if (selectedRole == UserRole.STUDENT) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F766E).copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Device Binding",
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Strict Single-Device Security",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E)
                            )
                            Text(
                                text = "Only one student login allowed per phone. Device hardware: ${viewModel.deviceModel}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Input Fields
            AnimatedVisibility(visible = isSignUp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text(if (selectedRole == UserRole.LECTURER) "Prof. John Doe" else "Jane Student") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = "Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_fullname_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            OutlinedTextField(
                value = identifier,
                onValueChange = { identifier = it },
                label = { Text(if (selectedRole == UserRole.LECTURER) "Email / Staff ID" else "Student Matric No") },
                placeholder = { Text(if (selectedRole == UserRole.LECTURER) "turing@university.edu" else "MAT/2026/0491") },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedRole == UserRole.LECTURER) Icons.Default.Email else Icons.Default.Fingerprint,
                        contentDescription = "Identifier"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_identifier_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Faculty Picker (Always visible on SignUp or can be viewed)
            if (isSignUp) {
                ExposedDropdownMenuBox(
                    expanded = facultyExpanded,
                    onExpandedChange = { facultyExpanded = !facultyExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedFaculty,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Faculty") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = "Faculty") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = facultyExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("auth_faculty_dropdown"),
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = facultyExpanded,
                        onDismissRequest = { facultyExpanded = false }
                    ) {
                        AVAILABLE_FACULTIES.forEach { fac ->
                            DropdownMenuItem(
                                text = { Text(fac, fontSize = 13.sp) },
                                onClick = {
                                    selectedFaculty = fac
                                    facultyExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Passcode Field
            OutlinedTextField(
                value = passcode,
                onValueChange = { passcode = it },
                label = { Text(if (isSignUp) "Create Login Passcode" else "Secret Passcode") },
                placeholder = { Text("4 to 6 digit PIN or secret code") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Passcode") },
                trailingIcon = {
                    IconButton(onClick = { passcodeVisible = !passcodeVisible }) {
                        Icon(
                            imageVector = if (passcodeVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Passcode Visibility"
                        )
                    }
                },
                visualTransformation = if (passcodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_passcode_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Error display
            if (authState is AuthState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Action Button
            Button(
                onClick = {
                    if (isSignUp) {
                        if (selectedRole == UserRole.LECTURER) {
                            viewModel.registerLecturer(fullName, identifier, selectedFaculty, passcode)
                        } else {
                            viewModel.registerStudent(fullName, identifier, selectedFaculty, passcode)
                        }
                    } else {
                        if (selectedRole == UserRole.LECTURER) {
                            viewModel.loginLecturer(identifier, passcode)
                        } else {
                            viewModel.loginStudent(identifier, passcode)
                        }
                    }
                },
                enabled = identifier.isNotBlank() && passcode.isNotBlank() && (!isSignUp || fullName.isNotBlank()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("auth_submit_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (isSignUp) "Create ${selectedRole.name.lowercase().replaceFirstChar { it.uppercase() }} Account" else "Log In to Portal",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Demo Helpers
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Demo Accounts Quick-Fill",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedRole = UserRole.LECTURER
                                isSignUp = false
                                identifier = "alan.turing@university.edu"
                                passcode = "1234"
                            },
                            modifier = Modifier.weight(1f).testTag("quick_fill_lecturer"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Lecturer Demo", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                selectedRole = UserRole.STUDENT
                                isSignUp = true
                                fullName = "Ada Lovelace"
                                identifier = "MAT/2026/0101"
                                passcode = "4321"
                                selectedFaculty = AVAILABLE_FACULTIES.first()
                            },
                            modifier = Modifier.weight(1f).testTag("quick_fill_student"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Student Demo", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
