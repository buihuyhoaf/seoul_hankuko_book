package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun QuizScreen(
    quizId: Int,
    onNavigateBack: () -> Unit
) {
    var currentQuestion by remember { mutableStateOf(1) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var quizCompleted by remember { mutableStateOf(false) }
    
    val totalQuestions = 5 // Mock total questions
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text("← Back")
            }
            
            Text(
                text = "🎯 Quiz $quizId",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(80.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Bar
        LinearProgressIndicator(
            progress = currentQuestion.toFloat() / totalQuestions,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        
        Text(
            text = "Question $currentQuestion of $totalQuestions",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (!quizCompleted) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Quiz Question $currentQuestion",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "What does 안녕하세요 mean?",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val answers = listOf("Hello", "Goodbye", "Thank you", "Please")
                        
                        answers.forEach { answer ->
                            Button(
                                onClick = { 
                                    selectedAnswer = answer
                                    isCorrect = answer == "Hello"
                                    showResult = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !showResult,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        showResult && answer == selectedAnswer -> 
                                            if (isCorrect) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.error
                                        showResult && answer == "Hello" -> 
                                            MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Text(
                                    answer,
                                    color = when {
                                        showResult && answer == selectedAnswer -> 
                                            if (isCorrect) MaterialTheme.colorScheme.onPrimary 
                                            else MaterialTheme.colorScheme.onError
                                        showResult && answer == "Hello" -> 
                                            MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                        
                        if (showResult) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = if (isCorrect) "✅ Correct!" else "❌ Wrong! The answer is 'Hello'",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { 
                                    if (currentQuestion < totalQuestions) {
                                        currentQuestion++
                                        selectedAnswer = null
                                        showResult = false
                                        isCorrect = false
                                    } else {
                                        quizCompleted = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (currentQuestion < totalQuestions) "Next Question" else "Complete Quiz"
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Quiz Completed Screen
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎉 Quiz Completed!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Great job! You've completed the quiz.",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onNavigateBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back to Lesson")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
