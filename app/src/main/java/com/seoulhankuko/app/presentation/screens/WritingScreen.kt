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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.seoulhankuko.app.data.api.model.ExerciseResponse
import com.seoulhankuko.app.presentation.viewmodel.LessonViewModel
import com.seoulhankuko.app.presentation.viewmodel.LessonUiState
import com.seoulhankuko.app.presentation.components.SouthKoreaLoadingIcon
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import com.seoulhankuko.app.presentation.utils.AppColors

/**
 * WritingScreen - Practice writing Korean
 */
@Composable
fun WritingScreen(
    lessonId: String,
    onNavigateBack: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(lessonId) {
        val currentLessonId = (viewModel.uiState.value as? LessonUiState.Success)
            ?.lessonWithChallenges
            ?.lesson
            ?.id

        if (currentLessonId != lessonId || viewModel.uiState.value !is LessonUiState.Success) {
            viewModel.loadLesson(lessonId)
        }
    }

    val writingExercise = when (val state = uiState) {
        is LessonUiState.Success -> state.lessonWithChallenges
            ?.exercisesResponse
            ?.firstOrNull { exercise ->
                val type = exercise.type.lowercase()
                type == "writing" || type == "writing_practice"
            }
        else -> null
    }

    when (val state = uiState) {
        is LessonUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LessonFlowColors.BackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                SouthKoreaLoadingIcon(size = 56.dp)
            }
        }

        is LessonUiState.Success -> {
            when {
                writingExercise != null -> {
                    WritingContent(
                        exercise = writingExercise,
                        onSubmit = { userAnswer ->
                            viewModel.submitExercise(
                                exerciseId = writingExercise.id,
                                lessonId = lessonId,
                                response = userAnswer
                            )
                            viewModel.updateLessonProgress(lessonId)
                            onNavigateBack()
                        }
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(LessonFlowColors.BackgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Không tìm thấy bài tập viết trong bài học này.",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                color = LessonFlowColors.TextPrimary
                            )
                            Button(
                                onClick = onNavigateBack,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = LessonFlowColors.PrimaryColor
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Quay lại",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.White
                                )
                            }
                        }
                    }
                }
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
fun WritingContent(
    exercise: ExerciseResponse,
    onSubmit: (String) -> Unit
) {
    var userInput by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LessonFlowColors.BackgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✍️ Viết",
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
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(LessonFlowColors.PrimaryColor, LessonFlowColors.SecondaryColor)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✍️", fontSize = 40.sp)
                }
                
                Text(
                    text = exercise.prompt ?: exercise.content ?: exercise.title.orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = LessonFlowColors.TextPrimary
                )
                
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = {
                        Text("Viết câu trả lời của bạn...")
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = false,
                    maxLines = 5
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        if (!exercise.sampleAnswer.isNullOrBlank()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = AppColors.GreenPrimary.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Gợi ý",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = LessonFlowColors.TextPrimary
                    )
                    Text(
                        text = exercise.sampleAnswer ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LessonFlowColors.TextSecondary
                    )
                }
            }
        }

        Button(
            onClick = { onSubmit(userInput) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LessonFlowColors.SuccessColor
            ),
            enabled = userInput.isNotBlank()
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


