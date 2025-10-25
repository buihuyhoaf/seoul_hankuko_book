package com.seoulhankuko.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.domain.model.TaskType
import com.seoulhankuko.app.presentation.screens.ChallengeScreen
import com.seoulhankuko.app.presentation.screens.CourseScreen
import com.seoulhankuko.app.presentation.screens.EntryTestResultScreen
import com.seoulhankuko.app.presentation.screens.EntryTestScreen
import com.seoulhankuko.app.presentation.screens.FirstScreen
import com.seoulhankuko.app.presentation.screens.LeaderboardScreen
import com.seoulhankuko.app.presentation.screens.LessonScreenProgressive
import com.seoulhankuko.app.presentation.screens.LoggedAccountsScreen
import com.seoulhankuko.app.presentation.screens.LoginScreen
import com.seoulhankuko.app.presentation.screens.ModernHomeScreen
import com.seoulhankuko.app.presentation.screens.NotificationScreen
import com.seoulhankuko.app.presentation.screens.ProfileScreen
import com.seoulhankuko.app.presentation.screens.QuestsScreen
import com.seoulhankuko.app.presentation.screens.QuizScreen
import com.seoulhankuko.app.presentation.screens.ShopScreen
import com.seoulhankuko.app.presentation.screens.UnitScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(
    initialDestination: String = "home"
) {
    val navController = rememberNavController()
    
    Logger.Navigation.navigationInitialized(initialDestination)
    
    NavHost(
        navController = navController,
        startDestination = initialDestination
    ) {
        // First/Landing Screen
        composable("home") {
            Logger.Navigation.navigateToHome()
            FirstScreen(
                onNavigateToLogin = { 
                    Logger.Navigation.navigateToLogin()
                    navController.navigate("login") 
                },
                onNavigateToRegister = { 
                    // Register functionality removed - redirect to login
                    Logger.Navigation.navigateToLogin()
                    navController.navigate("login") 
                },
                onNavigateToLearn = { courseId: Int ->
                    Logger.Navigation.navigateToLearn(courseId)
                    navController.navigate("course/$courseId")
                },
                onNavigateToEntryTest = {
                    // Flow A: New user - navigate to entry test
                    navController.navigate("entry-test")
                },
                onNavigateToGuestMode = {
                    // Flow C: Guest mode - navigate directly to courses (skip entry test)
                    navController.navigate("courses") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Login Screen with email parameter
        composable(
            route = "login/{email}",
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            Logger.Navigation.navigateToLogin()
            LoginScreen(
                initialEmail = email,
                onNavigateBack = { 
                    Logger.Navigation.backFromScreen("Login")
                    navController.popBackStack() 
                },
                onNavigateToLearn = { courseId: Int ->
                    Logger.Navigation.navigateToLearn(courseId)
                    navController.navigate("courses") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Login Screen without email parameter
        composable("login") {
            Logger.Navigation.navigateToLogin()
            LoginScreen(
                initialEmail = "",
                onNavigateBack = { 
                    Logger.Navigation.backFromScreen("Login")
                    navController.popBackStack() 
                },
                onNavigateToLearn = { courseId: Int ->
                    Logger.Navigation.navigateToLearn(courseId)
                    navController.navigate("courses") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Logged Accounts Screen
        composable("logged-accounts") {
            LoggedAccountsScreen(
                onAccountSelected = { account ->
                    // Navigate to login with pre-filled email when auto-login fails
                    val encodedEmail = URLEncoder.encode(account.email, StandardCharsets.UTF_8.toString())
                    navController.navigate("login/$encodedEmail") {
                        popUpTo("logged-accounts") { inclusive = false }
                    }
                },
                onSuccessfulAutoLogin = {
                    // Navigate to courses when auto-login succeeds
                    navController.navigate("courses") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onAddAccountClick = {
                    navController.navigate("login") {
                        popUpTo("logged-accounts") { inclusive = false }
                    }
                },
                onBackClick = {
                    // Prevent back navigation from LoggedAccountsScreen since it's the entry point
                    // when user has saved accounts but no valid session
                }
            )
        }
        
        
        // Home Screen (formerly Courses) - Now using Modern Design
        composable("courses") {
            ModernHomeScreen(
                onCourseSelected = { courseId: Int ->
                    navController.navigate("course/$courseId")
                },
                onNavigateToChallenge = {
                    navController.navigate("challenge")
                },
                onNavigateToNotification = {
                    navController.navigate("notification")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }
        
        // Challenge Screen
        composable("challenge") {
            ChallengeScreen(
                onNavigateToHome = {
                    navController.navigate("courses")
                },
                onNavigateToChallenge = { /* Current screen */ },
                onNavigateToNotification = {
                    navController.navigate("notification")
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }
        
        // Notification Screen
        composable("notification") {
            NotificationScreen(
                onNavigateToHome = {
                    navController.navigate("courses")
                },
                onNavigateToChallenge = {
                    navController.navigate("challenge")
                },
                onNavigateToNotification = { /* Current screen */ },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }
        
        // Profile Screen
        composable("profile") {
            ProfileScreen(
                onLogout = {
                    navController.navigate("logged-accounts") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate("courses")
                },
                onNavigateToChallenge = {
                    navController.navigate("challenge")
                },
                onNavigateToNotification = {
                    navController.navigate("notification")
                },
                onNavigateToProfile = { /* Current screen */ }
            )
        }
        
        // Course Screen
        composable("course/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            courseId?.let { id ->
                CourseScreen(
                    courseId = id,
                    onNavigateToUnit = { unitId: Int ->
                        navController.navigate("unit/$unitId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Unit Screen
        composable("unit/{unitId}") { backStackEntry ->
            val unitId = backStackEntry.arguments?.getString("unitId")?.toIntOrNull()
            unitId?.let { id ->
                UnitScreen(
                    unitId = id,
                    onNavigateToLesson = { lessonId: Int ->
                        navController.navigate("lesson/$lessonId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Lesson Screen
        composable("lesson/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull()
            lessonId?.let { id ->
                LessonScreenProgressive(
                    lessonId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onTaskClick = { taskType ->
                        when (taskType) {
                            TaskType.QUIZ -> {
                                navController.navigate("quiz/$id/lesson/$lessonId")
                            }
                            TaskType.LISTENING -> {
                                // Navigate to listening activity
                            }
                            TaskType.SPEAKING -> {
                                // Navigate to speaking activity
                            }
                            TaskType.WRITING -> {
                                // Navigate to writing activity
                            }
                        }
                    }
                )
            }
        }
        
        // Quiz Screen
        composable("quiz/{quizId}/lesson/{lessonId}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")?.toIntOrNull()
            val lessonId = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull()
            quizId?.let { id ->
                QuizScreen(
                    quizId = id,
                    lessonId = lessonId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Shop Screen
        composable("shop") {
            ShopScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Quests Screen
        composable("quests") {
            QuestsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Leaderboard Screen
        composable("leaderboard") {
            LeaderboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Entry Test Screen
        composable("entry-test") {
            EntryTestScreen(
                onNavigateToResult = { score, courseId, courseName ->
                    // Store data in current back stack entry and navigate
                    navController.currentBackStackEntry?.savedStateHandle?.set("entryTestScore", score)
                    navController.currentBackStackEntry?.savedStateHandle?.set("entryTestCourseId", courseId)
                    navController.currentBackStackEntry?.savedStateHandle?.set("entryTestCourseName", courseName)
                    navController.navigate("entry-test-result")
                },
                onNavigateToLogin = {
                    // Navigate to login screen
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = false }
                    }
                },
                onNavigateBack = { navController.popBackStack() },
                allowOfflineMode = true // Allow offline entry test
            )
        }
        
        // Entry Test Result Screen
        composable("entry-test-result") { backStackEntry ->
            // Get data from saved state handle, fallback to defaults if not found
            val score = backStackEntry.savedStateHandle.get<Float>("entryTestScore") ?: 0f
            val courseId = backStackEntry.savedStateHandle.get<Int>("entryTestCourseId") ?: 1
            val courseName = backStackEntry.savedStateHandle.get<String>("entryTestCourseName") ?: "Course"
            
            EntryTestResultScreen(
                score = score,
                courseId = courseId,
                courseName = courseName,
                onContinue = {
                    navController.navigate("courses") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
    }
}
