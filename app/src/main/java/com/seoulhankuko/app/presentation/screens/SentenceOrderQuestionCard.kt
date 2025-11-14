package com.seoulhankuko.app.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.seoulhankuko.app.R
import com.seoulhankuko.app.data.api.model.QuestionResponse
import com.seoulhankuko.app.domain.model.ChallengeWithOptions
import com.seoulhankuko.app.presentation.utils.LessonFlowColors
import kotlinx.coroutines.delay

@Composable
fun SentenceOrderQuestionCard(
    challenge: ChallengeWithOptions,
    question: QuestionResponse?,
    questionIndex: Int,
    answerStatus: AnswerStatus,
    onAnswerSubmitted: (Boolean, List<String>) -> Unit,
    onPlayAudio: (() -> Unit)? = null,
    onCompletedChanged: ((Boolean) -> Unit)? = null,
    triggerSubmit: Int = 0
) {
    // Lấy correct sequence từ challenge hoặc question
    val correctSequence = remember(challenge, question) {
        challenge.sentenceOrder?.correctSequence
            ?: question?.sentenceOrder?.correctSequence
            ?: emptyList()
    }
    
    // Shuffle để hiển thị trong word bank
    val shuffledWords = remember(correctSequence) {
        correctSequence.shuffled()
    }
    
    // State để lưu thứ tự user đã sắp xếp
    var userSequence by remember(correctSequence) {
        mutableStateOf<List<String?>>(List(correctSequence.size) { null })
    }
    
    // State để lưu các từ còn lại trong word bank
    var availableWords by remember(shuffledWords) {
        mutableStateOf(shuffledWords.toMutableList())
    }
    
    // Không cần drag and drop nữa
    
    // Kiểm tra xem đã hoàn thành chưa
    val isCompleted = remember(userSequence) {
        userSequence.all { it != null }
    }
    
    // Notify khi completed state thay đổi
    LaunchedEffect(isCompleted) {
        onCompletedChanged?.invoke(isCompleted)
    }
    
    // Kiểm tra đáp án đúng
    val isCorrect = remember(userSequence, correctSequence) {
        if (!isCompleted) false
        else userSequence.mapNotNull { it } == correctSequence
    }
    
    val isSubmitted = answerStatus != AnswerStatus.NONE
    
    // Handle trigger submit từ bên ngoài (khi button được click)
    LaunchedEffect(triggerSubmit) {
        if (triggerSubmit > 0 && isCompleted && !isSubmitted) {
            val sequence = userSequence.mapNotNull { it }
            onAnswerSubmitted(isCorrect, sequence)
        }
    }
    val scrollState = rememberScrollState()
    
    // Không auto submit nữa, user phải click button
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header với câu hỏi
                SentenceOrderHeader(
                    questionText = challenge.challenge.question,
                    questionIndex = questionIndex
                )
                
                // Image nếu có
                question?.imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Hình minh hoạ",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                
                // Answer Area - Khu vực sắp xếp
                AnswerArea(
                    userSequence = userSequence,
                    correctSequence = correctSequence,
                    isSubmitted = isSubmitted,
                    isCorrect = isCorrect,
                    draggedWord = null,
                    onSlotClick = { index ->
                        if (!isSubmitted) {
                            if (userSequence[index] != null) {
                                // Remove từ slot và trả về word bank
                                val word = userSequence[index]
                                userSequence = userSequence.toMutableList().apply {
                                    this[index] = null
                                }
                                // Tạo list mới để trigger recomposition
                                availableWords = (availableWords + word!!).toMutableList()
                            }
                        }
                    },
                    onSlotDrop = { _, _ -> }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Dragged word overlay - sẽ được hiển thị ở layer cao hơn
                
                // Word Bank - Các từ có sẵn
                WordBank(
                    words = availableWords,
                    isSubmitted = isSubmitted,
                    draggedWord = null,
                    dragOffset = Offset.Zero,
                    onWordClick = { word ->
                        if (!isSubmitted) {
                            // Tìm slot trống đầu tiên
                            val emptyIndex = userSequence.indexOfFirst { it == null }
                            if (emptyIndex != -1) {
                                userSequence = userSequence.toMutableList().apply {
                                    this[emptyIndex] = word
                                }
                                // Tạo list mới để trigger recomposition
                                availableWords = availableWords.filter { it != word }.toMutableList()
                            }
                        }
                    },
                    onWordDragStart = { },
                    onWordDrag = { },
                    onWordDragEnd = { }
                )
                
                // Button được quản lý bởi LessonFlowScreen
                
                // Explanation sau khi submit
                val explanationText = question?.explanation
                if (isSubmitted && !explanationText.isNullOrBlank()) {
                    Text(
                        text = "💡 Giải thích: $explanationText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LessonFlowColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SentenceOrderHeader(
    questionText: String,
    questionIndex: Int,
    showAudioIcon: Boolean = false,
    onPlayAudio: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Câu ${questionIndex + 1}",
            style = MaterialTheme.typography.titleSmall,
            color = LessonFlowColors.TextSecondary
        )
        
        Text(
            text = questionText,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = LessonFlowColors.TextPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AnswerArea(
    userSequence: List<String?>,
    correctSequence: List<String>,
    isSubmitted: Boolean,
    isCorrect: Boolean,
    draggedWord: String?,
    onSlotClick: (Int) -> Unit,
    onSlotDrop: (Int, String) -> Unit
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        userSequence.forEachIndexed { index, word ->
            val slotNumber = index + 1
            
            AnswerSlot(
                word = word,
                slotNumber = slotNumber,
                isCorrect = if (isSubmitted) correctSequence.getOrNull(index) == word else null,
                isSubmitted = isSubmitted,
                isDragOver = false,
                onClick = { onSlotClick(index) },
                onDrop = { }
            )
        }
    }
}

@Composable
private fun AnswerSlot(
    word: String?,
    slotNumber: Int,
    isCorrect: Boolean?,
    isSubmitted: Boolean,
    isDragOver: Boolean,
    onClick: () -> Unit,
    onDrop: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isSubmitted && isCorrect == true -> Color(0xFF4CAF50) // Green
            isSubmitted && isCorrect == false -> Color(0xFFE74C3C) // Red
            isDragOver -> LessonFlowColors.PrimaryColor.copy(alpha = 0.7f)
            word != null -> LessonFlowColors.PrimaryColor
            else -> Color(0xFFE0E0E0) // Light gray
        },
        animationSpec = tween(300)
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSubmitted && isCorrect == true -> Color(0xFFE8F5E9) // Light green
            isSubmitted && isCorrect == false -> Color(0xFFFFEBEE) // Light red
            word != null -> LessonFlowColors.PrimaryColor.copy(alpha = 0.1f)
            else -> Color(0xFFF5F5F5) // Very light gray
        },
        animationSpec = tween(300)
    )
    
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 80.dp, minHeight = 60.dp)
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .background(backgroundColor)
            .clickable(enabled = !isSubmitted || word != null) { 
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$slotNumber",
                style = MaterialTheme.typography.labelSmall,
                color = LessonFlowColors.TextSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            if (word != null) {
                Text(
                    text = word,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = LessonFlowColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LessonFlowColors.TextSecondary.copy(alpha = 0.5f)
                )
            }
            
            // Icon feedback
            if (isSubmitted && isCorrect != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCorrect) "✓" else "✗",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFE74C3C),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WordBank(
    words: List<String>,
    isSubmitted: Boolean,
    draggedWord: String?,
    dragOffset: Offset,
    onWordClick: (String) -> Unit,
    onWordDragStart: (String) -> Unit,
    onWordDrag: (Offset) -> Unit,
    onWordDragEnd: () -> Unit
) {
    val rowCount = (words.size + 2) / 3 // Tính số hàng (làm tròn lên)
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height((rowCount * 80).dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
    ) {
        items(words) { word ->
            var visible by remember { mutableStateOf(false) }
            
            LaunchedEffect(word) {
                delay(words.indexOf(word) * 100L)
                visible = true
            }
            
            AnimatedVisibility(
                visible = visible && !isSubmitted,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200))
            ) {
                WordChip(
                    word = word,
                    isDragging = false,
                    dragOffset = Offset.Zero,
                    onClick = { onWordClick(word) },
                    onDragStart = { },
                    onDrag = { },
                    onDragEnd = { }
                )
            }
        }
    }
}

@Composable
private fun WordChip(
    word: String,
    isDragging: Boolean,
    dragOffset: Offset,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LessonFlowColors.PrimaryColor.copy(alpha = 0.1f))
            .border(
                width = 1.5.dp,
                color = LessonFlowColors.PrimaryColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = word,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = LessonFlowColors.PrimaryColor,
            textAlign = TextAlign.Center
        )
    }
}

