package com.seoulhankuko.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LessonScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    onNavigateToQuiz: (quizId: Int) -> Unit
) {
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
                text = "📖 Lesson $lessonId",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.width(80.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Lesson Content
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📚 Lesson Content",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Korean Greetings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "In this lesson, you will learn basic Korean greetings and how to use them in different situations.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Key Vocabulary:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text("• 안녕하세요 (annyeonghaseyo) - Hello")
                    Text("• 안녕히 가세요 (annyeonghi gaseyo) - Goodbye (when someone is leaving)")
                    Text("• 감사합니다 (gamsahamnida) - Thank you")
                    Text("• 죄송합니다 (joesonghamnida) - Sorry")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { onNavigateToQuiz(lessonId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Start Quiz →",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}
