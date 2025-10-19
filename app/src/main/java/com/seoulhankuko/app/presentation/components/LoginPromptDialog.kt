package com.seoulhankuko.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LoginPromptDialog(
    show: Boolean,
    onLoginClick: () -> Unit,
    onRemindLaterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onRemindLaterClick,
            title = {
                Text(
                    text = "🎉 Great job!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You've completed the entry test! 🎯",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "\nTo save your progress and access personalized features, please sign in to your account.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Sign In Now")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onRemindLaterClick,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Remind Me Later")
                }
            },
            modifier = modifier
        )
    }
}


