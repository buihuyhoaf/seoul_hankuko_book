package com.seoulhankuko.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon

@Composable
fun BottomNavigationItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Color(0xFFFFEB3B), // Màu vàng khi được chọn
    unselectedColor: Color = Color.White,
    contentDescription: String? = null
) {
    // Animation cho scale khi được chọn
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 300, delayMillis = 0),
        label = "scale"
    )
    
    // Animation cho màu icon
    val animatedIconColor by animateColorAsState(
        targetValue = if (isSelected) selectedColor else unselectedColor,
        animationSpec = tween(durationMillis = 300),
        label = "iconColor"
    )
    
    TextButton(
        onClick = onClick,
        modifier = modifier.scale(scale)
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = animatedIconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
