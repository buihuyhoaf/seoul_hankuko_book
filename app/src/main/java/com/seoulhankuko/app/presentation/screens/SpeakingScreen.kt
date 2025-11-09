package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import com.seoulhankuko.app.presentation.utils.AppColors

/**
 * SpeakingScreen - Practice pronunciation
 */
@Composable
fun SpeakingScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRecording by remember { mutableStateOf(false) }
    
    when (val state = uiState) {
        is LessonUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LessonFlowColors.BackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = LessonFlowColors.PrimaryColor)
            }
        }
        
        is LessonUiState.Success -> {
            val exercises = state.lessonWithChallenges?.challenges?.filter { 
                it.challenge.type == com.seoulhankuko.app.domain.model.QuestionType.PRONUNCIATION
            } ?: emptyList()
            
            if (exercises.isNotEmpty()) {
                val exercise = exercises.first()
                SpeakingContent(
                    exercise = exercise,
                    isRecording = isRecording,
                    onRecordClick = { 
                        isRecording = !isRecording 
                        // TODO: Implement actual recording
                    },
                    onComplete = {
                        // Update progress and streak
                        viewModel.markExerciseCompletion(exercise.challenge.id, "speaking")
                        viewModel.updateLessonProgress(lessonId)
                        onNavigateBack()
                    }
                )
            } else {
                onNavigateBack()
            }
        }
        
        is LessonUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LessonFlowColors.BackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SpeakingContent(
    exercise: com.seoulhankuko.app.domain.model.ChallengeWithOptions,
    isRecording: Boolean,
    onRecordClick: () -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LessonFlowColors.BackgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎤 Luyện phát âm",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = LessonFlowColors.TextPrimary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(LessonFlowColors.PrimaryColor, LessonFlowColors.SecondaryColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎤", fontSize = 48.sp)
                }
                
                Text(
                    text = exercise.challenge.question,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = LessonFlowColors.TextPrimary
                )
                
                Button(
                    onClick = onRecordClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) LessonFlowColors.ErrorColor else LessonFlowColors.PrimaryColor
                    )
                ) {
                    Text(
                        text = if (isRecording) "Dừng" else "Thu âm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.White
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LessonFlowColors.SuccessColor
            )
        ) {
            Text(
                text = "Hoàn thành",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.White
            )
        }
    }
}


