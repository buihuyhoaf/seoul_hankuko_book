# 📱 Migration Guide: Next.js Duolingo Clone → Android Kotlin + Jetpack Compose

## 🎯 Tổng quan Migration

Dự án này sẽ được chuyển đổi từ **Next.js web application** sang **Android native app** sử dụng:
- **Kotlin** cho backend logic
- **Jetpack Compose** cho UI
- **Room Database** thay thế PostgreSQL
- **MVVM Architecture** với ViewModel và StateFlow

---

## 🏗️ Architecture Comparison

### Current Next.js Architecture
```
Frontend: React + Next.js 14
├── App Router với Server Components
├── Client Components với useState/useEffect
├── Zustand cho state management
├── Tailwind CSS cho styling
└── Shadcn UI components

Backend: Next.js API Routes
├── Server Actions cho mutations
├── Drizzle ORM với PostgreSQL
├── Clerk Authentication
└── Stripe Payments
```

### Target Android Architecture
```
UI Layer: Jetpack Compose
├── Composable functions
├── StateFlow/Flow cho reactive state
├── Material Design 3
└── Navigation Compose

Business Logic: MVVM
├── ViewModel với StateFlow
├── Repository pattern
├── Use Cases (optional)
└── Dependency Injection (Hilt)

Data Layer: Room Database
├── Entities (thay thế Drizzle schema)
├── DAOs (thay thế queries)
├── Database với migrations
└── Local data source
```

---

## 📊 Database Schema Migration

### PostgreSQL → Room Database

#### 1. Courses Table
```kotlin
// Room Entity
@Entity(tableName = "courses")
data class Course(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val imageSrc: String
)

// DAO
@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    suspend fun getAllCourses(): List<Course>
    
    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Int): Course?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)
}
```

#### 2. Units Table
```kotlin
@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Unit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val courseId: Int,
    val order: Int
)
```

#### 3. Lessons Table
```kotlin
@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = Unit::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Lesson(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val unitId: Int,
    val order: Int
)
```

#### 4. Challenges Table
```kotlin
enum class ChallengeType {
    SELECT, ASSIST
}

@Entity(
    tableName = "challenges",
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Challenge(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val lessonId: Int,
    val type: ChallengeType,
    val question: String,
    val order: Int
)
```

#### 5. Challenge Options Table
```kotlin
@Entity(
    tableName = "challenge_options",
    foreignKeys = [
        ForeignKey(
            entity = Challenge::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChallengeOption(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val challengeId: Int,
    val text: String,
    val correct: Boolean,
    val imageSrc: String? = null,
    val audioSrc: String? = null
)
```

#### 6. User Progress Table
```kotlin
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey
    val userId: String,
    val userName: String = "User",
    val userImageSrc: String = "/mascot.svg",
    val activeCourseId: Int? = null,
    val hearts: Int = 5,
    val points: Int = 0
)
```

#### 7. Challenge Progress Table
```kotlin
@Entity(
    tableName = "challenge_progress",
    foreignKeys = [
        ForeignKey(
            entity = Challenge::class,
            parentColumns = ["id"],
            childColumns = ["challengeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChallengeProgress(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String,
    val challengeId: Int,
    val completed: Boolean = false
)
```

---

## 🔄 State Management Migration

### Zustand → StateFlow/Flow

#### Current Zustand Store
```typescript
// use-practice-modal.ts
type PracticeState = {
  isOpen: boolean;
  open: () => void;
  close: () => void;
};

export const usePracticeModal = create<PracticeState>((set) => ({
  isOpen: false,
  open: () => set({ isOpen: true }),
  close: () => set({ isOpen: false }),
}));
```

#### Android StateFlow Equivalent
```kotlin
// PracticeModalViewModel.kt
class PracticeModalViewModel : ViewModel() {
    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()
    
    fun open() {
        _isOpen.value = true
    }
    
    fun close() {
        _isOpen.value = false
    }
}

// Usage in Compose
@Composable
fun PracticeModalScreen(
    viewModel: PracticeModalViewModel = hiltViewModel()
) {
    val isOpen by viewModel.isOpen.collectAsState()
    
    if (isOpen) {
        // Show modal
    }
}
```

---

## 🎨 UI Component Migration

### React Components → Jetpack Compose

#### Current React Component
```tsx
// user-progress.tsx
type Props = {
  activeCourse: Course;
  hearts: number;
  points: number;
  hasActiveSubscription: boolean;
};

export const UserProgress = ({ activeCourse, hearts, points, hasActiveSubscription }: Props) => {
  return (
    <div className="w-full">
      <div className="flex items-center justify-between gap-x-2 w-full">
        <Image src={activeCourse.imageSrc} alt={activeCourse.title} height={32} width={32} />
        <Progress value={progress} className="h-2" />
      </div>
    </div>
  );
};
```

#### Android Compose Equivalent
```kotlin
// UserProgress.kt
@Composable
fun UserProgress(
    activeCourse: Course,
    hearts: Int,
    points: Int,
    hasActiveSubscription: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = activeCourse.imageSrc,
                contentDescription = activeCourse.title,
                modifier = Modifier.size(32.dp)
            )
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.height(8.dp)
            )
        }
    }
}
```

---

## 🔐 Authentication Migration

### Clerk → Firebase Auth / Custom Auth

#### Current Clerk Implementation
```typescript
// queries.ts
export const getUserProgress = cache(async () => {
  const { userId } = await auth();
  if (!userId) return null;
  // ... rest of logic
});
```

#### Android Firebase Auth Equivalent
```kotlin
// AuthRepository.kt
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    suspend fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    suspend fun getUserProgress(): UserProgress? {
        val userId = getCurrentUserId() ?: return null
        return userProgressDao.getUserProgress(userId)
    }
}
```

---

## 💰 Payment Integration Migration

### Stripe → Google Play Billing

#### Current Stripe Implementation
```typescript
// user-subscription.ts
export const getUserSubscription = cache(async () => {
  const { userId } = await auth();
  if (!userId) return null;
  
  const data = await db.query.userSubscription.findFirst({
    where: eq(userSubscription.userId, userId),
  });
  
  const isActive = data.stripeCurrentPeriodEnd.getTime() + DAT_IN_MS > Date.now();
  return { ...data, isActive: !!isActive };
});
```

#### Android Google Play Billing Equivalent
```kotlin
// BillingRepository.kt
class BillingRepository @Inject constructor(
    private val billingClient: BillingClient
) {
    suspend fun getUserSubscription(): UserSubscription? {
        val userId = authRepository.getCurrentUserId() ?: return null
        
        val purchases = billingClient.queryPurchasesAsync(
            BillingClient.SkuType.SUBS
        ).purchasesList
        
        val activeSubscription = purchases.find { 
            it.purchaseState == Purchase.PurchaseState.PURCHASED 
        }
        
        return activeSubscription?.let { subscription ->
            UserSubscription(
                userId = userId,
                subscriptionId = subscription.orderId,
                isActive = true,
                expiryDate = subscription.purchaseTime
            )
        }
    }
}
```

---

## 🎵 Audio Integration Migration

### Web Audio → Android MediaPlayer/ExoPlayer

#### Current Web Audio Implementation
```tsx
// quiz.tsx
const [correctAudio, _c, correctControl] = useAudio({ src: "correct.wav" });
const [incorrectAudio, _i, incorrectControl] = useAudio({ src: "incorrect.wav" });

// Usage
correctControl.play();
incorrectControl.play();
```

#### Android Audio Equivalent
```kotlin
// AudioManager.kt
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val correctSound = MediaPlayer.create(context, R.raw.correct)
    private val incorrectSound = MediaPlayer.create(context, R.raw.incorrect)
    
    fun playCorrectSound() {
        correctSound.start()
    }
    
    fun playIncorrectSound() {
        incorrectSound.start()
    }
}
```

---

## 📱 Navigation Migration

### Next.js Router → Navigation Compose

#### Current Next.js Navigation
```tsx
// quiz.tsx
const router = useRouter();

const onCheck = () => router.push("/learn");
```

#### Android Navigation Compose Equivalent
```kotlin
// Navigation.kt
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "learn"
    ) {
        composable("learn") {
            LearnScreen(
                onNavigateToLesson = { lessonId ->
                    navController.navigate("lesson/$lessonId")
                }
            )
        }
        
        composable("lesson/{lessonId}") { backStackEntry ->
            val lessonId = backStackEntry.arguments?.getString("lessonId")?.toIntOrNull()
            lessonId?.let { id ->
                LessonScreen(
                    lessonId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
```

---

## 🎯 Key Migration Strategies

### 1. **Data Migration**
- Export data từ PostgreSQL
- Transform data format cho Room
- Import vào Android app

### 2. **UI/UX Adaptation**
- Convert web responsive design sang mobile-first
- Adapt touch interactions
- Optimize cho mobile performance

### 3. **Offline-First Approach**
- Room database cho local storage
- Sync mechanism với backend
- Handle offline scenarios

### 4. **Performance Optimization**
- Lazy loading với Compose
- Image caching với Coil
- Background processing với WorkManager

---

## 📋 Migration Checklist

### Phase 1: Foundation
- [ ] Setup Android project với Jetpack Compose
- [ ] Implement Room database schema
- [ ] Setup MVVM architecture với Hilt
- [ ] Implement basic navigation

### Phase 2: Core Features
- [ ] Migrate authentication system
- [ ] Implement course/lesson structure
- [ ] Create quiz/challenge system
- [ ] Add progress tracking

### Phase 3: Gamification
- [ ] Implement hearts system
- [ ] Add points/XP tracking
- [ ] Create leaderboard
- [ ] Add quests system

### Phase 4: Advanced Features
- [ ] Integrate Google Play Billing
- [ ] Add audio support
- [ ] Implement offline sync
- [ ] Add push notifications

### Phase 5: Polish & Testing
- [ ] UI/UX refinement
- [ ] Performance optimization
- [ ] Testing (Unit, Integration, UI)
- [ ] App store preparation

---

## 🚀 Next Steps

1. **Setup Android Studio** với Kotlin và Jetpack Compose
2. **Create new Android project** với proper architecture
3. **Implement Room database** với schema migration
4. **Start với core features** (courses, lessons, challenges)
5. **Gradually add gamification** features
6. **Test thoroughly** trên multiple devices

---

## 📚 Resources

- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Room Database Guide](https://developer.android.com/training/data-storage/room)
- [MVVM Architecture](https://developer.android.com/topic/architecture)
- [Google Play Billing](https://developer.android.com/google/play/billing)
- [Firebase Authentication](https://firebase.google.com/docs/auth)

---

*Document này sẽ được cập nhật khi có thêm thông tin về migration process.*

