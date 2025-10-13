# Seoul Hankuko API Updates

## Tổng quan
Dự án Seoul Hankuko đã được cập nhật để tương thích với API của Korean Learning backend. Các thay đổi bao gồm việc cập nhật endpoints, thêm các model mới và tạo các repository để quản lý dữ liệu.

## Các thay đổi chính

### 1. Cập nhật ApiService (`ApiService.kt`)
- **Thêm endpoint registration mới**: `/api/v1/register` thay vì chỉ có `/api/v1/user`
- **Cập nhật endpoint getCurrentUser**: `/api/v1/user/me/` thay vì `/api/v1/users/me`
- **Thêm các endpoint mới cho Korean Learning**:
  - Course management: `/api/v1/courses`, `/api/v1/courses/{course_id}`, `/api/v1/units/{unit_id}`, `/api/v1/lessons/{lesson_id}`
  - Quiz management: `/api/v1/quizzes/{quiz_id}`, `/api/v1/quizzes/{quiz_id}/attempt`
  - Exercise endpoints: `/api/v1/exercises/listening/{exercise_id}`, `/api/v1/exercises/speaking/{exercise_id}`, `/api/v1/exercises/writing/{exercise_id}`
  - User progress: `/api/v1/user/{username}/progress`, `/api/v1/user/{username}/course-progress`, `/api/v1/user/{username}/exp-history`
  - Daily goals: `/api/v1/user/{username}/daily-goals`
  - Gamification: `/api/v1/badges`, `/api/v1/leaderboard`, `/api/v1/user/{username}/friends`

### 2. Thêm Korean Learning API Models (`KoreanLearningApiModels.kt`)
Tạo file mới chứa tất cả các model cho Korean Learning API:

#### User Registration Models
- `UserRegistrationRequest`: Request cho việc đăng ký user mới
- `UserRegistrationResponse`: Response từ registration endpoint
- `UserRegistrationData`: Dữ liệu user được trả về

#### Course Management Models
- `CourseResponse`: Thông tin course cơ bản
- `CourseDetailResponse`: Chi tiết course với units
- `UnitResponse`, `UnitDetailResponse`: Thông tin unit
- `LessonResponse`, `LessonDetailResponse`: Thông tin lesson
- `CourseProgress`, `UnitProgress`, `LessonProgress`: Progress tracking

#### Quiz Management Models
- `QuizDetailResponse`: Chi tiết quiz với questions
- `QuestionResponse`: Thông tin question và options
- `QuizAttemptResponse`: Kết quả quiz attempt
- `QuestionResultResponse`: Kết quả từng question

#### Exercise Models
- `ListeningExerciseResponse`: Listening exercise
- `SpeakingExerciseResponse`: Speaking exercise  
- `WritingExerciseResponse`: Writing exercise

#### User Progress Models
- `UserProgressResponse`: Tổng quan progress của user
- `CourseProgressResponse`: Progress theo course
- `ExpLogResponse`: Lịch sử EXP
- `StreakUpdateResponse`: Cập nhật streak

#### Daily Goals Models
- `DailyGoalsResponse`: Mục tiêu hàng ngày
- `DailyGoalsUpdateResponse`: Cập nhật mục tiêu

#### Gamification Models
- `BadgeResponse`: Thông tin badge
- `LeaderboardEntryResponse`: Entry trong leaderboard
- `FriendResponse`: Thông tin friend

### 3. Cập nhật AuthRepository (`AuthRepository.kt`)
- **Thêm method `signUp()` mới**: Sử dụng `/api/v1/register` endpoint
- **Giữ lại method `createUser()`**: Sử dụng `/api/v1/user` endpoint (cho admin)
- **Import thêm**: `UserRegistrationRequest` model

### 4. Tạo các Repository mới

#### CourseRepository (`CourseRepository.kt`)
Quản lý dữ liệu course, unit, lesson:
- `getCourses()`: Lấy danh sách courses
- `getCourse()`: Lấy chi tiết course
- `getUnit()`: Lấy chi tiết unit
- `getLesson()`: Lấy chi tiết lesson
- StateFlow cho reactive updates

#### QuizRepository (`QuizRepository.kt`)
Quản lý quiz và exercises:
- `getQuiz()`: Lấy chi tiết quiz
- `submitQuizAttempt()`: Submit quiz attempt
- `getUserQuizAttempts()`: Lấy lịch sử quiz attempts
- `getListeningExercise()`, `getSpeakingExercise()`, `getWritingExercise()`: Lấy exercises
- StateFlow cho quiz state management

#### UserProgressRepository (`UserProgressRepository.kt`)
Quản lý progress và gamification:
- `getUserProgress()`: Lấy tổng quan progress
- `getUserCourseProgress()`: Lấy progress theo course
- `getUserExpHistory()`: Lấy lịch sử EXP
- `updateUserStreak()`: Cập nhật streak
- `getUserDailyGoals()`, `updateUserDailyGoals()`: Quản lý daily goals
- StateFlow cho progress tracking

#### GamificationRepository (`GamificationRepository.kt`)
Quản lý badges, leaderboard, friends:
- `getAllBadges()`: Lấy danh sách badges
- `getLeaderboard()`: Lấy leaderboard
- `getUserFriends()`: Lấy danh sách friends
- StateFlow cho gamification data

## Cách sử dụng

### 1. Authentication
```kotlin
// Sử dụng registration endpoint mới
authRepository.signUp(name, username, email, password)

// Hoặc sử dụng createUser endpoint (admin)
authRepository.createUser(name, username, email, password)
```

### 2. Course Management
```kotlin
// Inject CourseRepository
@Inject lateinit var courseRepository: CourseRepository

// Lấy danh sách courses
courseRepository.getCourses(page = 1, itemsPerPage = 10, token = currentToken)

// Lấy chi tiết course
courseRepository.getCourse(courseId = 1, token = currentToken)

// Observe courses state
courseRepository.courses.collect { courses ->
    // Update UI
}
```

### 3. Quiz Management
```kotlin
// Inject QuizRepository
@Inject lateinit var quizRepository: QuizRepository

// Lấy quiz
quizRepository.getQuiz(quizId = 1, token = currentToken)

// Submit quiz attempt
val answers = mapOf("question_1" to "answer_a", "question_2" to "answer_b")
quizRepository.submitQuizAttempt(quizId = 1, answers = answers, token = currentToken)

// Observe quiz state
quizRepository.currentQuiz.collect { quiz ->
    // Update UI
}
```

### 4. User Progress
```kotlin
// Inject UserProgressRepository
@Inject lateinit var userProgressRepository: UserProgressRepository

// Lấy user progress
userProgressRepository.getUserProgress(username = "user123", token = currentToken)

// Lấy daily goals
userProgressRepository.getUserDailyGoals(username = "user123", token = currentToken)

// Observe progress state
userProgressRepository.userProgress.collect { progress ->
    // Update UI
}
```

### 5. Gamification
```kotlin
// Inject GamificationRepository
@Inject lateinit var gamificationRepository: GamificationRepository

// Lấy badges
gamificationRepository.getAllBadges(page = 1, itemsPerPage = 20, token = currentToken)

// Lấy leaderboard
gamificationRepository.getLeaderboard(page = 1, itemsPerPage = 50, token = currentToken)

// Observe gamification state
gamificationRepository.badges.collect { badges ->
    // Update UI
}
```

## Lưu ý quan trọng

1. **Backward Compatibility**: Các endpoint cũ vẫn được giữ lại để đảm bảo tương thích ngược
2. **Error Handling**: Tất cả repository đều sử dụng `ExceptionMapper` để xử lý lỗi nhất quán
3. **State Management**: Sử dụng StateFlow để reactive updates
4. **Token Management**: Tất cả API calls đều hỗ trợ optional token parameter
5. **Pagination**: Hỗ trợ pagination cho các endpoint list

## Kế hoạch phát triển tiếp theo

1. **UI Integration**: Tích hợp các repository mới vào UI components
2. **Offline Support**: Thêm caching và offline support
3. **Real-time Updates**: Thêm WebSocket support cho real-time updates
4. **Testing**: Thêm unit tests cho các repository mới
5. **Documentation**: Cập nhật API documentation

