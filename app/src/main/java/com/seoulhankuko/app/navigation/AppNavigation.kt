package com.seoulhankuko.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.presentation.screens.HomeScreen
import com.seoulhankuko.app.presentation.screens.LoginScreen
import com.seoulhankuko.app.presentation.screens.CoursesScreen
import com.seoulhankuko.app.presentation.screens.LearnScreen
import com.seoulhankuko.app.presentation.screens.LessonScreen
import com.seoulhankuko.app.presentation.screens.ShopScreen
import com.seoulhankuko.app.presentation.screens.QuestsScreen
import com.seoulhankuko.app.presentation.screens.LeaderboardScreen

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
        // Home/Landing Screen
        composable("home") {
            Logger.Navigation.navigateToHome()
            HomeScreen(
                onNavigateToLogin = { 
                    Logger.Navigation.navigateToLogin()
                    navController.navigate("login") 
                },
                onNavigateToRegister = { 
                    // Register functionality removed - redirect to login
                    Logger.Navigation.navigateToLogin()
                    navController.navigate("login") 
                },
                onNavigateToLearn = { courseId ->
                    Logger.Navigation.navigateToLearn(courseId)
                    navController.navigate("learn/$courseId")
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
                onNavigateToLearn = { courseId ->
                    Logger.Navigation.navigateToLearn(courseId)
                    navController.navigate("learn/$courseId") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
        
        
        // Courses Screen
        composable("courses") {
            CoursesScreen(
                onCourseSelected = { courseId ->
                    navController.navigate("learn/$courseId")
                },
                onNavigateBack = { navController.popBackStack() },
                onLogout = { 
                    navController.navigate("login") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }
        
        // Learn Screen
        composable("learn/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            courseId?.let { id ->
                LearnScreen(
                    courseId = id,
                    onNavigateToLesson = { lessonId ->
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
