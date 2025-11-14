package com.seoulhankuko.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.seoulhankuko.app.core.Logger
import com.seoulhankuko.app.data.local.CurrentLessonData
import com.seoulhankuko.app.presentation.screens.CourseScreen
import com.seoulhankuko.app.presentation.screens.FirstScreen
import com.seoulhankuko.app.presentation.screens.LeaderboardScreen
import com.seoulhankuko.app.presentation.screens.LessonScreen
import com.seoulhankuko.app.presentation.screens.LessonFlowScreen
import com.seoulhankuko.app.presentation.screens.ListeningScreen
import com.seoulhankuko.app.presentation.screens.SpeakingScreen
import com.seoulhankuko.app.presentation.screens.WritingScreen
import com.seoulhankuko.app.presentation.screens.LoggedAccountsScreen
import com.seoulhankuko.app.presentation.screens.LoginScreen
import com.seoulhankuko.app.presentation.screens.ModernHomeScreen
import com.seoulhankuko.app.presentation.screens.HangulAlphabetScreen
import com.seoulhankuko.app.presentation.screens.MissionScreen
import com.seoulhankuko.app.presentation.screens.ProfileScreen
import com.seoulhankuko.app.presentation.screens.CanvasScreen
import com.seoulhankuko.app.ui.screen.canvas.HangulCanvasScreen
import com.seoulhankuko.app.presentation.screens.QuestsScreen
import com.seoulhankuko.app.presentation.screens.ShopScreen
import com.seoulhankuko.app.presentation.screens.UnitScreen
import com.seoulhankuko.app.presentation.screens.NotificationScreen
import com.seoulhankuko.app.presentation.screens.RankingScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(
    initialDestination: String = "home",
    resumeLesson: CurrentLessonData? = null,
    onResumeLessonConsumed: () -> Unit = {},
    notificationLessonId: String? = null
) {
    val navController = rememberNavController()
    
    Logger.Navigation.navigationInitialized(initialDestination)
    
    // Handle navigation to lesson from notification
    LaunchedEffect(notificationLessonId) {
        notificationLessonId?.let { lessonId ->
            // Navigate to lesson screen
            navController.navigate("lesson/$lessonId") {
                popUpTo("courses") { inclusive = false }
                launchSingleTop = true
            }
        }
    }
    
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
                onCourseSelected = { courseId: String ->
                    navController.navigate("course/$courseId")
                },
                onNavigateToNotification = {
                    navController.navigate("notifications") {
                        launchSingleTop = true
                    }
                },
                onNavigateToAlphabet = {
                    navController.navigate("alphabet_list") {
                        launchSingleTop = true
                    }
                },
                onNavigateToRanking = {
                    navController.navigate("ranking") {
                        launchSingleTop = true
                    }
                },
                onNavigateToMission = {
                    navController.navigate("missions") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                }
            )
        }
        
        // Notification Screen
        composable("notifications") {
            NotificationScreen(
                onNavigateToHome = {
                    navController.navigate("courses") {
                        popUpTo("courses") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToAlphabet = {
                    navController.navigate("alphabet_list") {
                        launchSingleTop = true
                    }
                },
                onNavigateToRanking = {
                    navController.navigate("ranking") {
                        launchSingleTop = true
                    }
                },
                onNavigateToMission = {
                    navController.navigate("missions") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Hangul Alphabet Selection Screen
        composable("alphabet_list") {
            HangulAlphabetScreen(
                onNavigateToHome = {
                    navController.navigate("courses")
                },
                onNavigateToAlphabet = { /* Current screen */ },
                onNavigateToRanking = {
                    navController.navigate("ranking") {
                        launchSingleTop = true
                    }
                },
                onNavigateToMission = {
                    navController.navigate("missions") {
                        launchSingleTop = true
                    }
                },
                onNavigateToNotification = {
                    navController.navigate("notifications") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile")
                },
                onNavigateToPractice = {
                    navController.navigate("canvas")
                }
            )
        }

        // Ranking Screen placeholder
        composable("ranking") {
            RankingScreen(
                onNavigateToHome = {
                    navController.navigate("courses") {
                        popUpTo("courses") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToAlphabet = {
                    navController.navigate("alphabet_list") {
                        launchSingleTop = true
                    }
                },
                onNavigateToRanking = { /* Already here */ },
                onNavigateToMission = {
                    navController.navigate("missions") {
                        launchSingleTop = true
                    }
                },
                onNavigateToNotification = {
                    navController.navigate("notifications") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Mission Screen placeholder
        composable("missions") {
            MissionScreen(
                onNavigateToHome = {
                    navController.navigate("courses") {
                        popUpTo("courses") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToAlphabet = {
                    navController.navigate("alphabet_list") {
                        launchSingleTop = true
                    }
                },
                onNavigateToRanking = {
                    navController.navigate("ranking") {
                        launchSingleTop = true
                    }
                },
                onNavigateToMission = { /* Already here */ },
                onNavigateToNotification = {
                    navController.navigate("notifications") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                }
            )
        }

        // Canvas Screen for free-mode Hangul practice
        composable("canvas") {
            HangulCanvasScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Stroke Practice Screen (guided practice with auto-correction)
        composable("practice/{char}") { backStackEntry ->
            val encodedChar = backStackEntry.arguments?.getString("char") ?: ""
            val charSymbol = java.net.URLDecoder.decode(encodedChar, StandardCharsets.UTF_8.toString())
            com.seoulhankuko.app.ui.screen.canvas.StrokePracticeScreen(
                character = charSymbol,
                onBack = { navController.popBackStack() }
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
                    navController.navigate("courses") {
                        popUpTo("courses") { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNavigateToAlphabet = {
                    navController.navigate("alphabet_list") {
                        launchSingleTop = true
                    }
                },
                onNavigateToRanking = {
                    navController.navigate("ranking") {
                        launchSingleTop = true
                    }
                },
                onNavigateToMission = {
                    navController.navigate("missions") {
                        launchSingleTop = true
                    }
                },
                onNavigateToNotification = {
                    navController.navigate("notifications") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = { /* Already here */ },
                onNavigateBack = {
                    navController.popBackStack()
                },
                onAvatarClick = {
                    navController.navigate("profile") {
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Course Screen
        composable("course/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")
            courseId?.let { id ->
                CourseScreen(
                    courseId = id,
                    onNavigateToUnit = { unitId: String ->
                        navController.navigate("unit/$unitId?courseId=$id")
                    },
                    onNavigateToHome = {
                        navController.navigate("courses") {
                            popUpTo("courses") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToNotification = {
                        navController.navigate("notifications") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAlphabet = {
                        navController.navigate("alphabet_list") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRanking = {
                        navController.navigate("ranking") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToMission = {
                        navController.navigate("missions") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Unit Screen
        composable(
            route = "unit/{unitId}?courseId={courseId}",
            arguments = listOf(
                navArgument("unitId") { type = NavType.StringType },
                navArgument("courseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val unitId = backStackEntry.arguments?.getString("unitId")
            unitId?.let { id ->
                val parentCourseId = backStackEntry.arguments?.getString("courseId")
                UnitScreen(
                    unitId = id,
                    initialCourseId = parentCourseId,
                    onNavigateToLesson = { lessonId: String, courseId: String? ->
                        val effectiveCourseId = parentCourseId ?: courseId
                        val unitQuery = "?unitId=$id"
                        val courseQuery = effectiveCourseId?.let { "&courseId=$it" } ?: ""
                        navController.navigate("lesson/$lessonId$unitQuery$courseQuery")
                    },
                    onNavigateToHome = {
                        navController.navigate("courses") {
                            popUpTo("courses") { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                    onNavigateToNotification = {
                        navController.navigate("notifications") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToAlphabet = {
                        navController.navigate("alphabet_list") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRanking = {
                        navController.navigate("ranking") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToMission = {
                        navController.navigate("missions") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToProfile = {
                        navController.navigate("profile") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateBack = { fallbackCourseId ->
                        val previousRoute = navController.previousBackStackEntry?.destination?.route.orEmpty()
                        val cameFromCourse = previousRoute.startsWith("course")
                        val effectiveCourseId = parentCourseId ?: fallbackCourseId

                        val didPop = when {
                            cameFromCourse -> navController.popBackStack()
                            effectiveCourseId != null -> navController.popBackStack("course/$effectiveCourseId", inclusive = false)
                            else -> navController.popBackStack()
                        }

                        if (!didPop && effectiveCourseId != null) {
                            navController.navigate("course/$effectiveCourseId") {
                                popUpTo("courses") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
        
        // Lesson Screen
        composable(
            route = "lesson/{lessonId}?unitId={unitId}&courseId={courseId}",
            arguments = listOf(
                navArgument("lessonId") { type = NavType.StringType },
                navArgument("unitId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("courseId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            lessonId?.let { id ->
                val parentUnitId = backStackEntry.arguments?.getString("unitId")
                val parentCourseId = backStackEntry.arguments?.getString("courseId")
                LessonScreen(
                    lessonId = id,
                    courseId = parentCourseId,
                    onNavigateBack = {
                        val previousRoute = navController.previousBackStackEntry?.destination?.route.orEmpty()
                        val cameFromUnit = previousRoute.startsWith("unit")

                        val targetUnitRoute = when {
                            parentUnitId != null && parentCourseId != null -> "unit/$parentUnitId?courseId=$parentCourseId"
                            parentUnitId != null -> "unit/$parentUnitId"
                            else -> null
                        }

                        val didPop = when {
                            cameFromUnit -> navController.popBackStack()
                            targetUnitRoute != null -> navController.popBackStack(targetUnitRoute, inclusive = false)
                            else -> navController.popBackStack()
                        }

                        if (!didPop && parentUnitId != null) {
                            val navigateRoute = buildString {
                                append("unit/$parentUnitId")
                                if (parentCourseId != null) append("?courseId=$parentCourseId")
                            }
                            navController.navigate(navigateRoute) {
                                popUpTo("courses") { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    },
                    onNavigateToLessonFlow = {
                        navController.navigate("lesson-flow/$id")
                    },
                    onNavigateToListening = { exerciseId ->
                        // Pass both lessonId and exerciseId to ensure we can reload if needed
                        val currentLessonId = (navController.currentBackStackEntry?.arguments?.getString("lessonId")) ?: id
                        navController.navigate("listening/$exerciseId/lesson/$currentLessonId")
                    },
                    onNavigateToSpeaking = { exerciseId ->
                        // TODO: When SpeakingScreen is updated to accept exerciseId,
                        // change route to "speaking/{exerciseId}" and pass exerciseId
                        // For now, use lessonId which is available in this scope
                        navController.navigate("speaking/$id")
                    },
                    onNavigateToWriting = { exerciseId ->
                        // TODO: When WritingScreen is updated to accept exerciseId,
                        // change route to "writing/{exerciseId}" and pass exerciseId
                        // For now, use lessonId which is available in this scope
                        navController.navigate("writing/$id")
                    }
                )
            }
        }
        
        // Lesson Flow Screen (Practice Questions)
        composable("lesson-flow/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            lessonId?.let { id ->
                LessonFlowScreen(
                    lessonId = id,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToListening = { exerciseId ->
                        navController.navigate("listening/$exerciseId/lesson/$id")
                    }
                )
            }
        }
        
        // Listening Screen - receives exerciseId and optionally lessonId
        composable(
            route = "listening/{exerciseId}/lesson/{lessonId}",
            arguments = listOf(
                navArgument("exerciseId") { type = NavType.StringType },
                navArgument("lessonId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId")
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            if (exerciseId != null && lessonId != null) {
                ListeningScreen(
                    exerciseId = exerciseId,
                    lessonId = lessonId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Speaking Screen - TODO: Update to receive exerciseId like ListeningScreen
        composable("speaking/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            lessonId?.let { id ->
                SpeakingScreen(
                    lessonId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        // Writing Screen - TODO: Update to receive exerciseId like ListeningScreen
        composable("writing/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")
            lessonId?.let { id ->
                WritingScreen(
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

    LaunchedEffect(resumeLesson) {
        val lesson = resumeLesson ?: return@LaunchedEffect

        navController.navigate("courses") {
            popUpTo("courses") { inclusive = false }
            launchSingleTop = true
        }

        lesson.courseId?.let { courseId ->
            navController.navigate("course/$courseId") {
                launchSingleTop = true
            }
        }

        lesson.unitId?.let { unitId ->
            val courseIdQuery = lesson.courseId?.let { "?courseId=$it" } ?: ""
            navController.navigate("unit/$unitId$courseIdQuery") {
                launchSingleTop = true
            }
        }

        onResumeLessonConsumed()
    }
}
