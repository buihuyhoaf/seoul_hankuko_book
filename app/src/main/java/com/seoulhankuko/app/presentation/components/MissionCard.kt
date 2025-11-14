package com.seoulhankuko.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    
    val cardColor = if (mission.isCompleted) {
        Color(0xFF4CAF50) // Green
    } else {
        Color.White
    }
    
    val textColor = if (mission.isCompleted) {
        Color.White
    } else {
        Color.Black
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = getMissionTypeLabel(mission.type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 18.sp
            )
            
            if (!mission.isCompleted) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF2196F3),
                    trackColor = Color(0xFFE0E0E0)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${mission.progress}/${mission.target}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontSize = 14.sp
                )
                
                if (mission.isCompleted) {
                    Text(
                        text = "✓ Hoàn thành",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun getMissionTypeLabel(type: String): String {
    return when (type) {
        "lesson" -> "📚 Học bài"
        "speaking" -> "🎤 Luyện nói"
        "listening" -> "👂 Luyện nghe"
        else -> type
    }
}

