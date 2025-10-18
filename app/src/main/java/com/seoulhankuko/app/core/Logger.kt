package com.seoulhankuko.app.core

import timber.log.Timber
import com.seoulhankuko.app.domain.exception.AppException

/**
 * Centralized logging utility for SeoulHankukoBook app
 * Organized by screens and use cases
 */
object Logger {

    // ==================== APPLICATION LIFECYCLE ====================
    
    fun appStarted() {
        Timber.tag("APP_LIFECYCLE").d("Application started")
    }
    
    fun appDestroyed() {
        Timber.tag("APP_LIFECYCLE").d("Application destroyed")
    }

    // ==================== MAIN ACTIVITY ====================
    
    object MainActivity {
        private const val TAG = "MAIN_ACTIVITY"
        
        fun onCreate() {
            Timber.tag(TAG).d("onCreate: Activity created")
        }
        
        fun onResume() {
            Timber.tag(TAG).d("onResume: Activity resumed")
        }
        
        fun onPause() {
            Timber.tag(TAG).d("onPause: Activity paused")
        }
        
        fun setContentView() {
            Timber.tag(TAG).d("setContentView: Setting up Compose content")
        }
    }

    // ==================== HOME SCREEN ====================
    
    object HomeScreen {
        private const val TAG = "HOME_SCREEN"
        
        fun screenLoaded() {
            Timber.tag(TAG).d("Screen loaded")
        }
        
        fun authStateChanged(state: String) {
            Timber.tag(TAG).d("Auth state changed: $state")
        }
        
        fun navigateToLoginClicked() {
            Timber.tag(TAG).d("Navigate to Login button clicked")
        }
        
        fun navigateToRegisterClicked() {
            Timber.tag(TAG).d("Navigate to Register button clicked")
        }
        
        fun continueLearningClicked() {
            Timber.tag(TAG).d("Continue Learning button clicked")
        }
        
        fun signOutClicked() {
            Timber.tag(TAG).d("Sign Out button clicked")
        }
    }

    // ==================== LOGIN SCREEN ====================
    
    object LoginScreen {
        private const val TAG = "LOGIN_SCREEN"
        
        fun screenLoaded() {
            Timber.tag(TAG).d("Screen loaded")
        }
        
        fun signInButtonClicked(email: String) {
            Timber.tag(TAG).d("Sign In button clicked for email: $email")
        }
        
        fun signInSuccess() {
            Timber.tag(TAG).d("Sign in successful, navigating to learn")
        }
        
        fun signInFailed(error: String) {
            Timber.tag(TAG).e("Sign in failed: $error")
        }
        
        fun backButtonClicked() {
            Timber.tag(TAG).d("Back button clicked")
        }
        
        fun validationFailed(reason: String) {
            Timber.tag(TAG).w("Validation failed: $reason")
        }
        
        fun loadingStateChanged(isLoading: Boolean) {
            Timber.tag(TAG).d("Loading state changed: $isLoading")
        }
    }

    // ==================== REGISTER SCREEN ====================
    
    object RegisterScreen {
        private const val TAG = "REGISTER_SCREEN"
        
        fun screenLoaded() {
            Timber.tag(TAG).d("Screen loaded")
        }
        
        fun registerButtonClicked(username: String, email: String) {
            Timber.tag(TAG).d("Register button clicked for user: $username ($email)")
        }
        
        fun registrationSuccess() {
            Timber.tag(TAG).d("Registration successful, auto-signing in")
        }
        
        fun registrationFailed(error: String) {
            Timber.tag(TAG).e("Registration failed: $error")
        }
        
        fun backButtonClicked() {
            Timber.tag(TAG).d("Back button clicked")
        }
        
        fun validationFailed(reason: String) {
            Timber.tag(TAG).w("Validation failed: $reason")
        }
        
        fun loadingStateChanged(isLoading: Boolean) {
            Timber.tag(TAG).d("Loading state changed: $isLoading")
        }
    }

    // ==================== NAVIGATION ====================
    
    object Navigation {
        private const val TAG = "NAVIGATION"
        
        fun navigationInitialized(startDestination: String) {
            Timber.tag(TAG).d("Navigation initialized with start destination: $startDestination")
        }
        
        fun navigateToHome() {
            Timber.tag(TAG).d("Navigate to Home screen")
        }
        
        fun navigateToLogin() {
            Timber.tag(TAG).d("Navigate to Login screen")
        }
        
        fun navigateToRegister() {
            Timber.tag(TAG).d("Navigate to Register screen")
        }
        
        fun navigateToLearn(courseId: Int) {
            Timber.tag(TAG).d("Navigate to Learn screen with courseId: $courseId")
        }
        
        fun navigateToLesson(lessonId: Int) {
            Timber.tag(TAG).d("Navigate to Lesson screen with lessonId: $lessonId")
        }
        
        fun navigateToCourses() {
            Timber.tag(TAG).d("Navigate to Courses screen")
        }
        
        fun navigateToShop() {
            Timber.tag(TAG).d("Navigate to Shop screen")
        }
        
        fun navigateToQuests() {
            Timber.tag(TAG).d("Navigate to Quests screen")
        }
        
        fun navigateToLeaderboard() {
            Timber.tag(TAG).d("Navigate to Leaderboard screen")
        }
        
        fun backFromScreen(screenName: String) {
            Timber.tag(TAG).d("Back from $screenName screen")
        }
        
        fun backStackCleared() {
            Timber.tag(TAG).d("Back stack cleared")
        }
    }

    // ==================== AUTHENTICATION USE CASE ====================
    
    object AuthenticationUseCase {
        private const val TAG = "AUTH_USE_CASE"
        
        fun checkAuthState() {
            Timber.tag(TAG).d("Checking authentication state")
        }
        
        fun foundStoredToken() {
            Timber.tag(TAG).d("Found stored token, verifying validity")
        }
        
        fun noStoredToken() {
            Timber.tag(TAG).d("No stored token found, user signed out")
        }
        
        fun tokenVerified(userId: String) {
            Timber.tag(TAG).d("Token verified, user signed in with ID: $userId")
        }
        
        fun signInAttempt(email: String) {
            Timber.tag(TAG).d("Sign in attempt for email: $email")
        }
        
        fun signInApiCall() {
            Timber.tag(TAG).d("Making sign in API call")
        }
        
        fun signInApiSuccess() {
            Timber.tag(TAG).d("Sign in API call successful")
        }
        
        fun signInApiFailed(error: String, code: Int) {
            Timber.tag(TAG).e("Sign in API call failed: $error (Code: $code)")
        }
        
        fun tokenReceived() {
            Timber.tag(TAG).d("Access token received, storing securely")
        }
        
        fun fetchingUserInfo() {
            Timber.tag(TAG).d("Fetching user information")
        }
        
        fun userInfoRetrieved(username: String, userId: Int) {
            Timber.tag(TAG).d("User info retrieved: $username (ID: $userId)")
        }
        
        fun userInfoFetchFailed() {
            Timber.tag(TAG).w("Could not fetch user info, but login successful")
        }
        
        fun signUpAttempt(username: String, email: String) {
            Timber.tag(TAG).d("Sign up attempt for user: $username ($email)")
        }
        
        fun signUpApiCall() {
            Timber.tag(TAG).d("Making sign up API call")
        }
        
        fun signUpApiSuccess() {
            Timber.tag(TAG).d("Sign up API call successful")
        }
        
        fun signUpApiFailed(error: String, code: Int) {
            Timber.tag(TAG).e("Sign up API call failed: $error (Code: $code)")
        }
        
        fun userCreated(username: String, userId: Int) {
            Timber.tag(TAG).d("User created successfully: $username (ID: $userId)")
        }
        
        fun autoSignInAfterRegistration() {
            Timber.tag(TAG).d("Auto-signing in after successful registration")
        }
        
        fun signOutAttempt() {
            Timber.tag(TAG).d("Sign out attempt")
        }
        
        fun signOutApiCall() {
            Timber.tag(TAG).d("Making sign out API call")
        }
        
        fun signOutApiSuccess() {
            Timber.tag(TAG).d("Sign out API call successful")
        }
        
        fun signOutApiFailed(error: String) {
            Timber.tag(TAG).w("Sign out API call failed: $error, continuing with local logout")
        }
        
        fun clearingAuthData() {
            Timber.tag(TAG).d("Clearing stored authentication data")
        }
        
        fun signOutComplete() {
            Timber.tag(TAG).d("Sign out completed successfully")
        }
        
        fun storingToken() {
            Timber.tag(TAG).d("Storing authentication token")
        }
        
        fun storingUserId(userId: String) {
            Timber.tag(TAG).d("Storing user ID: $userId")
        }
        
        fun clearingStoredAuth() {
            Timber.tag(TAG).d("Clearing stored authentication data")
        }
        
        fun exceptionOccurred(operation: String, exception: AppException) {
            val exceptionType = when (exception) {
                is AppException.NetworkException -> "Network"
                is AppException.AuthException -> "Authentication"
                is AppException.DataException -> "Data"
                is AppException.ValidationException -> "Validation"
                is AppException.UnexpectedError -> "Unexpected"
            }
            Timber.tag(TAG).e(exception, "Exception occurred during $operation ($exceptionType): ${exception.message}")
        }
        
        fun exceptionOccurred(operation: String, exception: Throwable) {
            Timber.tag(TAG).e(exception, "Exception occurred during $operation: ${exception.message}")
        }
    }

    // ==================== API USE CASE ====================
    
    object ApiUseCase {
        private const val TAG = "API_USE_CASE"
        
        fun networkRequest(url: String, method: String) {
            Timber.tag(TAG).d("Network request: $method $url")
        }
        
        fun networkResponseSuccess(url: String, code: Int) {
            Timber.tag(TAG).d("Network response success: $url (Code: $code)")
        }
        
        fun networkResponseFailed(url: String, code: Int, error: String) {
            Timber.tag(TAG).e("Network response failed: $url (Code: $code) - $error")
        }
        
        fun networkException(url: String, exception: Throwable) {
            Timber.tag(TAG).e(exception, "Network exception for: $url")
        }
        
        fun tokenRefresh() {
            Timber.tag(TAG).d("Refreshing access token")
        }
        
        fun tokenRefreshSuccess() {
            Timber.tag(TAG).d("Token refresh successful")
        }
        
        fun tokenRefreshFailed() {
            Timber.tag(TAG).e("Token refresh failed")
        }
        
        fun rateLimitExceeded(endpoint: String) {
            Timber.tag(TAG).w("Rate limit exceeded for endpoint: $endpoint")
        }
        
        fun networkTimeout(url: String) {
            Timber.tag(TAG).w("Network timeout for: $url")
        }
    }

    // ==================== DATA STORAGE USE CASE ====================
    
    object DataStorageUseCase {
        private const val TAG = "DATA_STORAGE_USE_CASE"
        
        fun storingData(key: String, type: String) {
            Timber.tag(TAG).d("Storing $type data with key: $key")
        }
        
        fun retrievingData(key: String, type: String) {
            Timber.tag(TAG).d("Retrieving $type data with key: $key")
        }
        
        fun dataRetrieved(key: String, type: String, success: Boolean) {
            Timber.tag(TAG).d("Data retrieval $key ($type): ${if (success) "success" else "failed"}")
        }
        
        fun clearingData(type: String) {
            Timber.tag(TAG).d("Clearing $type data")
        }
        
        fun dataCleared(type: String) {
            Timber.tag(TAG).d("$type data cleared successfully")
        }
        
        fun storageException(operation: String, exception: Throwable) {
            Timber.tag(TAG).e(exception, "Storage exception during $operation")
        }
    }

    // ==================== COURSE USE CASE ====================
    
    object CourseUseCase {
        private const val TAG = "COURSE_USE_CASE"
        
        fun loadCourses() {
            Timber.tag(TAG).d("Loading courses")
        }
        
        fun coursesLoaded(count: Int) {
            Timber.tag(TAG).d("Courses loaded successfully: $count courses")
        }
        
        fun courseSelected(courseId: Int) {
            Timber.tag(TAG).d("Course selected: ID $courseId")
        }
        
        fun courseLoadFailed(error: String) {
            Timber.tag(TAG).e("Course load failed: $error")
        }
        
        fun loadCourseDetails(courseId: Int) {
            Timber.tag(TAG).d("Loading course details for ID: $courseId")
        }
        
        fun courseDetailsLoaded(courseId: Int) {
            Timber.tag(TAG).d("Course details loaded for ID: $courseId")
        }
    }

    // ==================== LESSON USE CASE ====================
    
    object LessonUseCase {
        private const val TAG = "LESSON_USE_CASE"
        
        fun loadLessons(courseId: Int) {
            Timber.tag(TAG).d("Loading lessons for course ID: $courseId")
        }
        
        fun lessonsLoaded(courseId: Int, count: Int) {
            Timber.tag(TAG).d("Lessons loaded for course $courseId: $count lessons")
        }
        
        fun lessonSelected(lessonId: Int) {
            Timber.tag(TAG).d("Lesson selected: ID $lessonId")
        }
        
        fun lessonLoadFailed(courseId: Int, error: String) {
            Timber.tag(TAG).e("Lesson load failed for course $courseId: $error")
        }
        
        fun startLesson(lessonId: Int) {
            Timber.tag(TAG).d("Starting lesson: ID $lessonId")
        }
        
        fun lessonCompleted(lessonId: Int) {
            Timber.tag(TAG).d("Lesson completed: ID $lessonId")
        }
        
        fun lessonProgress(lessonId: Int, progress: Int, total: Int) {
            Timber.tag(TAG).d("Lesson progress: ID $lessonId - $progress/$total")
        }
    }

    // ==================== QUIZ USE CASE ====================
    
    object QuizUseCase {
        private const val TAG = "QUIZ_USE_CASE"
        
        fun loadQuiz(lessonId: Int) {
            Timber.tag(TAG).d("Loading quiz for lesson ID: $lessonId")
        }
        
        fun quizLoaded(lessonId: Int, questionCount: Int) {
            Timber.tag(TAG).d("Quiz loaded for lesson $lessonId: $questionCount questions")
        }
        
        fun questionAnswered(questionId: Int, answer: String, correct: Boolean) {
            Timber.tag(TAG).d("Question answered: ID $questionId, Answer: $answer, Correct: $correct")
        }
        
        fun quizCompleted(lessonId: Int, score: Int, total: Int) {
            Timber.tag(TAG).d("Quiz completed for lesson $lessonId: Score $score/$total")
        }
        
        fun quizLoadFailed(lessonId: Int, error: String) {
            Timber.tag(TAG).e("Quiz load failed for lesson $lessonId: $error")
        }
    }

    // ==================== USER PROGRESS USE CASE ====================
    
    object UserProgressUseCase {
        private const val TAG = "USER_PROGRESS_USE_CASE"
        
        fun loadUserProgress(userId: String) {
            Timber.tag(TAG).d("Loading user progress for user: $userId")
        }
        
        fun userProgressLoaded(userId: String) {
            Timber.tag(TAG).d("User progress loaded for user: $userId")
        }
        
        fun updateProgress(userId: String, lessonId: Int) {
            Timber.tag(TAG).d("Updating progress for user $userId, lesson $lessonId")
        }
        
        fun progressUpdated(userId: String, lessonId: Int) {
            Timber.tag(TAG).d("Progress updated for user $userId, lesson $lessonId")
        }
        
        fun progressLoadFailed(userId: String, error: String) {
            Timber.tag(TAG).e("Progress load failed for user $userId: $error")
        }
    }

    // ==================== GOOGLE SIGN-IN ====================
    
    object GoogleSignIn {
        private const val TAG = "GOOGLE_SIGN_IN"
        
        fun signInAttempt(email: String) {
            Timber.tag(TAG).d("Google Sign-In attempt for email: $email")
        }
        
        fun signInSuccess(email: String) {
            Timber.tag(TAG).d("Google Sign-In successful for email: $email")
        }
        
        fun signInError(error: String) {
            Timber.tag(TAG).e("Google Sign-In error: $error")
        }
        
        fun signOutAttempt() {
            Timber.tag(TAG).d("Google Sign-Out attempt")
        }
        
        fun signOutSuccess() {
            Timber.tag(TAG).d("Google Sign-Out successful")
        }
        
        fun signOutError(error: String) {
            Timber.tag(TAG).e("Google Sign-Out error: $error")
        }
        
        fun tokenReceived() {
            Timber.tag(TAG).d("Google ID token received")
        }
        
        fun tokenValidationFailed() {
            Timber.tag(TAG).e("Google ID token validation failed")
        }
        
        fun apiCallStarted() {
            Timber.tag(TAG).d("Calling backend API with Google token")
        }
        
        fun apiCallSuccess() {
            Timber.tag(TAG).d("Backend API call successful")
        }
        
        fun apiCallFailed(error: String) {
            Timber.tag(TAG).e("Backend API call failed: $error")
        }
        
        fun userDataSaved() {
            Timber.tag(TAG).d("User data saved to local storage")
        }
        
        fun userDataCleared() {
            Timber.tag(TAG).d("User data cleared from local storage")
        }
        
        fun logoutApiCallStarted() {
            Timber.tag(TAG).d("Calling backend logout API")
        }
        
        fun logoutApiCallSuccess() {
            Timber.tag(TAG).d("Backend logout API call successful")
        }
        
        fun logoutApiCallError(error: String) {
            Timber.tag(TAG).e("Backend logout API call failed: $error")
        }
    }
}
