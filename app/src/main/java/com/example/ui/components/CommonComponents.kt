package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.GeolocationHelper
import kotlinx.coroutines.delay

/**
 * Animated Geolocation Radar showing real-time proximity to Lecturer (2m threshold)
 */
@Composable
fun RadarProximityView(
    currentDistanceMeters: Double,
    modifier: Modifier = Modifier,
    isSimulating: Boolean = false
) {
    val isWithinRange = currentDistanceMeters <= GeolocationHelper.MAX_ATTENDANCE_RADIUS_METERS

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse1"
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse2"
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val radarColor = if (isWithinRange) Color(0xFF10B981) else Color(0xFFF59E0B)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "Radar",
                        tint = radarColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2.0m Proximity Radar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isWithinRange) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isWithinRange) "IN 2M RANGE" else "OUT OF RANGE",
                        color = if (isWithinRange) Color(0xFF059669) else Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0B132B)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f

                    // Grid concentric rings
                    for (i in 1..4) {
                        val ringRadius = maxRadius * (i / 4f)
                        drawCircle(
                            color = Color(0xFF1E293B),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                    }

                    // 2m Range Boundary ring (emphasized with dash/glow)
                    val rangeBoundaryRadius = maxRadius * 0.45f
                    drawCircle(
                        color = radarColor.copy(alpha = 0.7f),
                        radius = rangeBoundaryRadius,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )

                    // Crosshair axes
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(0f, center.y),
                        end = Offset(size.width, center.y),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = Color(0xFF1E293B),
                        start = Offset(center.x, 0f),
                        end = Offset(center.x, size.height),
                        strokeWidth = 1f
                    )

                    // Expanding radar waves
                    drawCircle(
                        color = radarColor.copy(alpha = (1f - pulse1) * 0.4f),
                        radius = maxRadius * pulse1,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = radarColor.copy(alpha = (1f - pulse2) * 0.4f),
                        radius = maxRadius * pulse2,
                        center = center,
                        style = Stroke(width = 2f)
                    )

                    // Lecturer Beacon at Center
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = 12f,
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = center
                    )

                    // Student dot position calculated relative to distance
                    // Normalized to canvas: 2.0m sits at rangeBoundaryRadius
                    val normalizedDistRadius = (currentDistanceMeters / 2.0 * rangeBoundaryRadius)
                        .coerceAtMost(maxRadius.toDouble() * 0.92).toFloat()
                    val studentAngleRad = Math.toRadians(45.0)
                    val studentPos = Offset(
                        x = (center.x + normalizedDistRadius * kotlin.math.cos(studentAngleRad)).toFloat(),
                        y = (center.y + normalizedDistRadius * kotlin.math.sin(studentAngleRad)).toFloat()
                    )

                    // Student dot
                    drawCircle(
                        color = radarColor,
                        radius = 10f,
                        center = studentPos
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = studentPos
                    )
                }

                // Center floating distance readout
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    border = BorderStroke(1.dp, radarColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = GeolocationHelper.formatDistance(currentDistanceMeters),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "target: <= 2.0m",
                            color = Color.LightGray,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isWithinRange) {
                    "✓ Position verified within 2m of lecturer podium."
                } else {
                    "⚠ Walk closer to the lecturer's desk (Current: ${GeolocationHelper.formatDistance(currentDistanceMeters)})"
                },
                color = if (isWithinRange) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Biometric Face Scanner component with animated scan laser & challenge verification
 */
@Composable
fun BiometricFaceScanner(
    isScanning: Boolean,
    isVerified: Boolean,
    onScanComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "face_scan")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    LaunchedEffect(isScanning) {
        if (isScanning && !isVerified) {
            delay(2400)
            onScanComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A)),
        contentAlignment = Alignment.Center
    ) {
        // Face outline canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val faceColor = if (isVerified) Color(0xFF10B981) else Color(0xFF38BDF8)

            // Oval face contour
            drawOval(
                color = faceColor.copy(alpha = 0.6f),
                topLeft = Offset(center.x - 70.dp.toPx(), center.y - 85.dp.toPx()),
                size = Size(140.dp.toPx(), 170.dp.toPx()),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                )
            )

            // Corner brackets
            val bracketLen = 24.dp.toPx()
            val bracketColor = if (isVerified) Color(0xFF10B981) else Color(0xFF38BDF8)
            val left = center.x - 85.dp.toPx()
            val right = center.x + 85.dp.toPx()
            val top = center.y - 100.dp.toPx()
            val bottom = center.y + 100.dp.toPx()

            // Top Left
            drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), 3.dp.toPx(), StrokeCap.Round)
            drawLine(bracketColor, Offset(left, top), Offset(left, top + bracketLen), 3.dp.toPx(), StrokeCap.Round)

            // Top Right
            drawLine(bracketColor, Offset(right, top), Offset(right - bracketLen, top), 3.dp.toPx(), StrokeCap.Round)
            drawLine(bracketColor, Offset(right, top), Offset(right, top + bracketLen), 3.dp.toPx(), StrokeCap.Round)

            // Bottom Left
            drawLine(bracketColor, Offset(left, bottom), Offset(left + bracketLen, bottom), 3.dp.toPx(), StrokeCap.Round)
            drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - bracketLen), 3.dp.toPx(), StrokeCap.Round)

            // Bottom Right
            drawLine(bracketColor, Offset(right, bottom), Offset(right - bracketLen, bottom), 3.dp.toPx(), StrokeCap.Round)
            drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - bracketLen), 3.dp.toPx(), StrokeCap.Round)

            // Laser scan bar
            if (isScanning && !isVerified) {
                val currentLaserY = top + (bottom - top) * laserY
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF00F5FF), Color.White, Color(0xFF00F5FF), Color.Transparent)
                    ),
                    start = Offset(left, currentLaserY),
                    end = Offset(right, currentLaserY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Center status icons & texts
        if (isVerified) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981).copy(alpha = 0.2f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Biometric Match Confirmed",
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "Facial contour & liveness passed",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        } else if (isScanning) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Scanning Facial Landmarks...",
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = "Please keep your face steady inside the frame",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "Face Scan",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Facial Biometric Verification Required",
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}
