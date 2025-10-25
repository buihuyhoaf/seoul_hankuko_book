package com.seoulhankuko.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
fun EntryTestReminderDialog(
    show: Boolean,
    onStartEntryTest: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (show) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = {
                Text(
                    text = "🎯 Entry Test Required",
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
                        text = "You haven't completed the entry test yet!",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "\nTake the entry test to get personalized learning recommendations and start your Korean journey!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onStartEntryTest,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Start Entry Test")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Maybe Later")
                }
            },
            modifier = modifier
        )
    }
}









