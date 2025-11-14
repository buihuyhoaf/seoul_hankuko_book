package com.seoulhankuko.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.presentation.utils.ProfileColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

data class DailyExpData(
    val day: LocalDate,
    val exp: Int
)

@Composable
fun WeeklyExpChart(
    weeklyData: List<DailyExpData>,
    modifier: Modifier = Modifier
) {
    val visibilityState = remember { 
        MutableTransitionState(false).apply { targetState = true } 
    }
    
    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut(tween(180))
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "EXP trong tuần",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProfileColors.TextPrimary
                    )
                    
                    // Hiển thị khoảng thời gian của tuần
                    if (weeklyData.isNotEmpty()) {
                        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
                        val weekEnd = weekStart.plusDays(6)
                        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
                        Text(
                            text = "Tuần từ ${weekStart.format(dateFormatter)} đến ${weekEnd.format(dateFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ProfileColors.TextSecondary
                        )
                    }
                }
                
                if (weeklyData.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa có dữ liệu",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ProfileColors.TextSecondary
                        )
                    }
                } else {
                    WeeklyExpBarChart(
                        data = weeklyData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyExpBarChart(
    data: List<DailyExpData>,
    modifier: Modifier = Modifier
) {
    val maxExp = max(1, data.maxOfOrNull { it.exp } ?: 1)
    val chartHeight = 150.dp
    val barWidth = 40.dp
    val spacing = 8.dp
    
    // Tạo dữ liệu cho 7 ngày (từ thứ 2 đến chủ nhật)
    val weekDays = (1..7).map { DayOfWeek.of(it) }
    val fullWeekData = weekDays.map { dayOfWeek ->
        val date = LocalDate.now().with(DayOfWeek.MONDAY).plusDays(dayOfWeek.ordinal.toLong())
        data.find { it.day == date } ?: DailyExpData(date, 0)
    }
    
    Column(modifier = modifier) {
        // Biểu đồ
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val totalBarWidth = (barWidth.toPx() * 7) + (spacing.toPx() * 6)
                val startX = (canvasWidth - totalBarWidth) / 2
                
                fullWeekData.forEachIndexed { index, dailyData ->
                    val barHeight = if (maxExp > 0) {
                        (dailyData.exp.toFloat() / maxExp) * (canvasHeight - 40.dp.toPx())
                    } else {
                        0f
                    }
                    
                    val x = startX + (index * (barWidth.toPx() + spacing.toPx()))
                    val y = canvasHeight - 40.dp.toPx() - barHeight
                    
                    // Gradient cho cột
                    val gradient = Brush.verticalGradient(
                        colors = listOf(
                            ProfileColors.AccentGreen,
                            ProfileColors.SecondaryGreen.copy(alpha = 0.7f)
                        ),
                        startY = y,
                        endY = y + barHeight
                    )
                    
                    // Vẽ cột
                    drawRoundRect(
                        brush = gradient,
                        topLeft = Offset(x, y),
                        size = Size(barWidth.toPx(), barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )
                    
                    // Vẽ giá trị EXP trên cột
                    if (dailyData.exp > 0) {
                        drawIntoCanvas { canvas ->
                            val textPaint = android.graphics.Paint().apply {
                                textSize = 10.dp.toPx()
                                color = android.graphics.Color.parseColor("#333333")
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            
                            canvas.nativeCanvas.drawText(
                                dailyData.exp.toString(),
                                x + barWidth.toPx() / 2,
                                y - 4.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }
            }
        }
        
        // Nhãn ngày trong tuần - align với các cột
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                fullWeekData.forEachIndexed { index, dailyData ->
                    val dayLabel = when (dailyData.day.dayOfWeek) {
                        DayOfWeek.MONDAY -> "T2"
                        DayOfWeek.TUESDAY -> "T3"
                        DayOfWeek.WEDNESDAY -> "T4"
                        DayOfWeek.THURSDAY -> "T5"
                        DayOfWeek.FRIDAY -> "T6"
                        DayOfWeek.SATURDAY -> "T7"
                        DayOfWeek.SUNDAY -> "CN"
                    }
                    
                    Box(
                        modifier = Modifier.width(barWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = ProfileColors.TextSecondary
                        )
                    }
                    
                    // Thêm spacing giữa các nhãn (trừ cột cuối)
                    if (index < fullWeekData.size - 1) {
                        Spacer(modifier = Modifier.width(spacing))
                    }
                }
            }
        }
    }
}

