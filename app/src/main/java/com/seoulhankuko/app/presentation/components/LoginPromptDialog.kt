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
                    text = "🎉 Làm tốt lắm!",
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
                        text = "Bạn đã hoàn thành bài kiểm tra đầu vào! 🎯",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "\nĐể lưu tiến độ và truy cập các tính năng cá nhân hóa, vui lòng đăng nhập vào tài khoản của bạn.",
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
                    Text("Đăng nhập ngay")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onRemindLaterClick,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text("Nhắc tôi sau")
                }
            },
            modifier = modifier
        )
    }
}












