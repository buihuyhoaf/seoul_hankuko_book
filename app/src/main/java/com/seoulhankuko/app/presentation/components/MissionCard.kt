package com.seoulhankuko.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seoulhankuko.app.data.api.model.MissionResponse

@Composable
fun MissionCard(
    mission: MissionResponse,
    modifier: Modifier = Modifier
) {
    val progress = if (mission.target > 0) {
        mission.progress.toFloat() / mission.target.toFloat()
    } else {
        0f
    }
    
    // Animate progress bar
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )
    
    // Animate card scale when completed (from 0.97f → 1f with overshoot)
    var shouldScaleUp by remember(mission.isCompleted) { 
        mutableStateOf(mission.isCompleted) 
    }
    
    LaunchedEffect(mission.isCompleted) {
        if (mission.isCompleted && !shouldScaleUp) {
            shouldScaleUp = true
        }
    }
    
    val cardScale by animateFloatAsState(
        targetValue = if (shouldScaleUp) 1f else 0.97f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "cardScale"
    )
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(cardScale),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (mission.isCompleted) 0.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (mission.isCompleted) {
                        Modifier.background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFA8E6CF),
                                    Color(0xFF81D4FA)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Modifier
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE5E7EB),
                                shape = RoundedCornerShape(16.dp)
                            )
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon & Title row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = getMissionTypeLabel(mission.type),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151),
                        fontSize = 15.sp
                    )
                }
                
                // Progress bar (only show if not completed)
                if (!mission.isCompleted) {
                    LinearProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFF4CAF50),
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
                
                // Progress text + completion badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!mission.isCompleted) {
                        Text(
                            text = "${mission.progress}/${mission.target}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF4B5563),
                            fontSize = 13.sp
                        )
                    } else {
                        // Empty space on left when completed
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                    
                    // Completion badge (only show when completed)
                    if (mission.isCompleted) {
                        CompletionBadge()
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletionBadge() {
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300),
        label = "badgeAlpha"
    )
    
    Box(
        modifier = Modifier
            .alpha(alpha)
            .background(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Hoàn thành",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2E7D32),
            fontSize = 12.sp
        )
    }
}

private fun getMissionTypeLabel(type: String): String {
    return when (type) {
        "lesson" -> "Học bài"
        "speaking" -> "Luyện nói"
        "listening" -> "Luyện nghe"
        else -> type
    }
}

