package com.seoulhankuko.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.presentation.screens.FirstScreen
import com.seoulhankuko.app.presentation.screens.LoginScreen
import com.seoulhankuko.app.presentation.screens.HomeScreen
import com.seoulhankuko.app.presentation.screens.CourseScreen
import com.seoulhankuko.app.presentation.screens.UnitScreen
import com.seoulhankuko.app.presentation.screens.LessonScreen
import com.seoulhankuko.app.presentation.screens.QuizScreen
import com.seoulhankuko.app.presentation.screens.ShopScreen
import com.seoulhankuko.app.presentation.screens.QuestsScreen
import com.seoulhankuko.app.presentation.screens.LeaderboardScreen
import com.seoulhankuko.app.presentation.screens.ChallengeScreen
import com.seoulhankuko.app.presentation.screens.NotificationScreen
import com.seoulhankuko.app.presentation.screens.ProfileScreen

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
                }
            )
        }
        
        // Login Screen
        composable("login") {
            Logger.Navigation.navigateToLogin()
            LoginScreen(
                onNavigateBack = { 
                    Logger.Navigation.backFromScreen("Login")
                    navController.popBackStack() 
                },
                onNavigateToLearn = { courseId: Int ->
                    Logger.Navigation.navigateToLearn(courseId)
                    navController.navigate("course/$courseId") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
        
        
        // Home Screen (formerly Courses)
        composable("courses") {
            HomeScreen(
                onCourseSelected = { courseId: Int ->
                    navController.navigate("course/$courseId")
                },
                onNavigateBack = { navController.popBackStack() },
                onLogout = { 
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = false }
                    }
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
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = false }
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
                LessonScreen(
                    lessonId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuiz = { quizId: Int ->
                        navController.navigate("quiz/$quizId")
                    }
                )
            }
        }
        
        // Quiz Screen
        composable("quiz/{quizId}") { backStackEntry ->
            val quizId = backStackEntry.arguments?.getString("quizId")?.toIntOrNull()
            quizId?.let { id ->
                QuizScreen(
                    quizId = id,
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
    }
}
