package com.seoulhankuko.app.ui.screen.canvas

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.seoulhankuko.app.R

/**
 * Controls row for canvas actions
 */
@Composable
fun CanvasControls(
    showTemplate: Boolean,
    onToggleTemplate: () -> Unit,
    onClear: () -> Unit,
    onAnalyze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show Template Toggle
        IconButton(onClick = onToggleTemplate) {
            Icon(
                painter = painterResource(
                    id = if (showTemplate) R.drawable.visibility_off else R.drawable.visibility
                ),
                contentDescription = if (showTemplate) "Ẩn mẫu" else "Hiện mẫu"
            )
        }
        
        // Clear Button
        Button(
            onClick = onClear,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Xóa")
        }
        
        // Check/Analyze Button
        Button(
            onClick = onAnalyze,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Kiểm tra")
        }
    }
}

