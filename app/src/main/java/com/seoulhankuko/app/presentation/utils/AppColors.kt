package com.seoulhankuko.app.presentation.utils

import androidx.compose.ui.graphics.Color

/**
 * Central color constants file for the entire app
 * All colors used across screens should be defined here
 */

// ==========================================
// GENERAL THEME COLORS
// ==========================================
object AppColors {
    // Standard Material colors
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Gray = Color(0xFF9E9E9E)
    val LightGray = Color(0xFFE0E0E0)
    val DarkGray = Color(0xFF757575)
    
    // Green theme (primary app color)
    val GreenLight = Color(0xFFE8F5E8)
    val GreenPrimary = Color(0xFF4CAF50)
    val GreenMedium = Color(0xFF66BB6A)
    val GreenDark = Color(0xFF388E3C)
    val GreenDarkest = Color(0xFF1B5E20)
    val GreenMediumDark = Color(0xFF2E7D32)
}

// ==========================================
// HOME SCREEN COLORS (ModernHomeScreen)
// ==========================================
object HomeColors {
    val DuolingoGreen = Color(0xFF58CC02)
    val DuolingoDarkGreen = Color(0xFF1B5E20)
    val DuolingoLightGreen = Color(0xFFE8F5E8)
    val DuolingoGray = Color(0xFF7F8C8D)
    val DuolingoLightGray = Color(0xFFF8F9FA)
}

// ==========================================
// LESSON SCREEN COLORS (LessonScreen)
// ==========================================
object LessonColors {
    val HeaderGradientStart = Color(0xFF9FA8DA)
    val HeaderGradientEnd = Color(0xFF7986CB)
    val QuestionColor = Color(0xFF7C83FD)
    val ListeningColor = Color(0xFF5DA9E9)
    val SpeakingColor = Color(0xFFFFA62B)
    val PronunciationColor = Color(0xFFE56B6F)
    val WritingColor = Color(0xFF4CAF50)
    val CompletedGreen = Color(0xFF00C853)
    val ConnectorLineColor = Color(0xFFDADCE0)
    val BackgroundWhite = Color(0xFFF9FAFB)
    val CardBackground = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF212121)
    val TextSecondary = Color(0xFF757575)
    val Accent = Color(0xFF7C83FD)
    val AccentSoft = Color(0xFFE8E9FF)

    // Backward compatibility for existing usages
    val TopBarGradientStart = HeaderGradientStart
    val TopBarGradientEnd = HeaderGradientEnd
}

// ==========================================
// QUIZ SCREEN COLORS (QuizScreen)
// ==========================================
object QuizColors {
    val Primary = Color(0xFF4A90E2)
    val SurfaceVariant = Color(0xFFF6F8FB)
    val Error = Color(0xFFE74C3C)
    val OnPrimary = Color.White
    val Success = Color(0xFF27AE60)
    val Background = Color(0xFFFAFBFC)
    val TextPrimary = Color(0xFF2C3E50)
    val TextSecondary = Color(0xFF7F8C8D)
}

// ==========================================
// LESSON FLOW COLORS (LessonFlowScreen, Speaking, Writing, Listening)
// ==========================================
object LessonFlowColors {
    val PrimaryColor = Color(0xFFFF6F61)
    val SecondaryColor = Color(0xFFFFE0B2)
    val BackgroundColor = Color(0xFFFFF8E7)
    val SuccessColor = Color(0xFF4CAF50)
    val ErrorColor = Color(0xFFF44336)
    val TextPrimary = Color(0xFF333333)
    val TextSecondary = Color(0xFF757575)
}

// ==========================================
// PROFILE SCREEN COLORS (ProfileScreen)
// ==========================================
object ProfileColors {
    val PrimaryGreen = Color(0xFF4CAF50)
    val SecondaryGreen = Color(0xFF81C784)
    val AccentGreen = Color(0xFF388E3C)
    val BackgroundLight = Color(0xFFF9FFF9)
    val TextPrimary = Color(0xFF1B5E20)
    val TextSecondary = Color(0xFF4E4E4E)
    val GradientStart = Color(0xFF81C784)
    val GradientEnd = Color(0xFF66BB6A)
}

// ==========================================
// UNIT/COURSE SCREEN COLORS (UnitScreen, CourseScreen)
// ==========================================
object UnitColors {
    val SoftIndigo = Color(0xFF5A67D8)
    val CoolGray = Color(0xFFA0AEC0)
    val WarmOrange = Color(0xFFF6AD55)
    val BackgroundLight = Color(0xFFF8FAFC)
    val TextPrimary = Color(0xFF1A202C)
    val TextSecondary = Color(0xFF4A5568)
    val Lavender = Color(0xFFE9D8FD)
    val LightGray = Color(0xFFEDF2F7)
}

// ==========================================
// COURSE SCREEN COLORS (CourseScreen - Vietnamese UI)
// ==========================================
object CourseColors {
    val Background = Color(0xFFF9FAFB)
    val Card = Color.White
    val TextPrimary = Color(0xFF1C1C1C)
    val TextSecondary = Color(0xFF6C757D)
    val Accent = Color(0xFF7C83FD)
    val AccentLight = Color(0xFFE8E9FF) // Light version of Accent for badges and gradients
    val Completed = Color(0xFF4CAF50)
}

// ==========================================
// NOTIFICATION SCREEN COLORS (NotificationScreen)
// ==========================================
object NotificationColors {
    val BackgroundGreen = Color(0xFF81C784)
    val TitleGreen = Color(0xFF1B5E20)
}

// ==========================================
// FIRST SCREEN COLORS (FirstScreen)
// ==========================================
object FirstScreenColors {
    val PrimaryButton = Color(0xFF58CC02)
    val SecondaryButton = Color(0xFF1CB0F6)
    val BorderGray = Color(0xFFE5E5E5)
    val ContinueButton = Color(0xFF1B5E20)
}

// ==========================================
// LOGIN SCREEN COLORS (LoginScreen)
// ==========================================
object LoginColors {
    val TitleGreen = Color(0xFF1B5E20)
    val SubtitleGreen = Color(0xFF2E7D32)
    val PrimaryButton = Color(0xFF4CAF50)
    val FocusedBorder = Color(0xFF4CAF50)
    val UnfocusedBorder = Color(0xFFE0E0E0)
    val Error = Color(0xFFD32F2F)
    val Disabled = Color(0xFFE0E0E0)
    val DisabledText = Color(0xFF9E9E9E)
    val CardBackground = Color(0xFFF8F9FA)
}

// ==========================================
// ENTRY TEST COLORS (EntryTestScreen)
// ==========================================
object EntryTestColors {
    val SubmitButton = Color(0xFF4CAF50)
}

// ==========================================
// BOTTOM NAVIGATION COLORS (BottomNavigationBar)
// ==========================================
object BottomNavColors {
    val ContainerGreen = Color(0xFF4CAF50)
    val SelectedYellow = Color(0xFFFFEB3B)
}

object ComponentColors {
    val AudioPlayerTint = Color(0xFF5EEAD4)
}

// ==========================================
// MISC COLORS
// ==========================================
object MiscColors {
    val ErrorRed = Color(0xFFD32F2F)
    val WarningOrange = Color(0xFFFF6F00)
    val Orange = Color(0xFFFF9800)
    val DeepOrange = Color(0xFFFF5722)
    val Amber = Color(0xFFFFC107)
    val SuccessGreen = Color(0xFF4CAF50)
    val InfoBlue = Color(0xFF2196F3)
    
    // Gradients
    val Gradient1 = listOf(Color(0xFFE8F5E8), Color(0xFFF0F8F0))
    val Gradient2 = listOf(Color(0xFFE3F2FD), Color(0xFFF3E5F5))
    val Gradient3 = listOf(Color(0xFFFFF3E0), Color(0xFFFFF8E1))
    val Gradient4 = listOf(Color(0xFFF1F8E9), Color(0xFFF9FBE7))
    val Gradient5 = listOf(Color(0xFFE8EAF6), Color(0xFFF3E5F5))
    val Gradient6 = listOf(Color(0xFFE0F2F1), Color(0xFFE8F5E8))
}

