package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

object BarcodeHelper {
    fun generateSessionCode(): String {
        return (100000..999999).random().toString()
    }
}

/**
 * Draws a clean, high-contrast, scalable 1D barcode pattern for the session
 */
@Composable
fun BarcodeView(
    data: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            val totalBars = 60
            val barWidth = size.width / (totalBars * 1.5f)
            val seed = data.hashCode().toLong()
            val rnd = Random(seed)

            var currentX = 0f
            for (i in 0 until totalBars) {
                val isThick = rnd.nextBoolean()
                val actualWidth = if (isThick) barWidth * 1.8f else barWidth
                val isSpace = i % 7 == 0 || (rnd.nextInt(5) == 0 && i > 3 && i < totalBars - 3)

                if (!isSpace) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(currentX, 0f),
                        size = Size(actualWidth, size.height)
                    )
                }
                currentX += actualWidth + (barWidth * 0.5f)
                if (currentX >= size.width) break
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "* $data *",
            color = Color.Black,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}

/**
 * Draws a clean, stylized 2D QR matrix with finder patterns
 */
@Composable
fun QrMatrixView(
    data: String,
    modifier: Modifier = Modifier,
    matrixColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    val gridSize = 21
    val seed = data.hashCode().toLong()
    val rnd = Random(seed)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / gridSize

            // Helper to draw QR finder pattern (top-left, top-right, bottom-left)
            fun drawFinder(startX: Int, startY: Int) {
                for (r in 0 until 7) {
                    for (c in 0 until 7) {
                        val isBorder = r == 0 || r == 6 || c == 0 || c == 6
                        val isCenter = r in 2..4 && c in 2..4
                        if (isBorder || isCenter) {
                            drawRect(
                                color = matrixColor,
                                topLeft = Offset((startX + c) * cellSize, (startY + r) * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }

            drawFinder(0, 0)
            drawFinder(gridSize - 7, 0)
            drawFinder(0, gridSize - 7)

            // Fill body data dots
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val inTopLeft = r < 7 && c < 7
                    val inTopRight = r < 7 && c >= gridSize - 7
                    val inBottomLeft = r >= gridSize - 7 && c < 7
                    if (!inTopLeft && !inTopRight && !inBottomLeft) {
                        val isFilled = rnd.nextBoolean()
                        if (isFilled) {
                            drawRect(
                                color = matrixColor,
                                topLeft = Offset(c * cellSize, r * cellSize),
                                size = Size(cellSize * 0.92f, cellSize * 0.92f)
                            )
                        }
                    }
                }
            }
        }
    }
}
