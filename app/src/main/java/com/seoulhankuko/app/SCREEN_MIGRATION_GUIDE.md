# 📱 Guide Migration Từng Màn Hình: Next.js → Android Kotlin + Jetpack Compose

## 🎯 Tổng Quan Dự Án

Dự án Duolingo Next.js có **tổng cộng 8 màn hình chính** cần migration sang Android:

### 📊 Danh Sách Màn Hình

| # | Màn Hình | Route | Mô Tả | Độ Ưu Tiên |
|---|----------|-------|--------|------------|
| 1 | **Home/Landing** | `/` | Trang chủ với đăng nhập/đăng ký | 🔴 Cao |
| 2 | **Courses** | `/courses` | Danh sách khóa học ngôn ngữ | 🔴 Cao |
| 3 | **Learn** | `/learn` | Màn hình học chính với units/lessons | 🔴 Cao |
| 4 | **Lesson** | `/lesson/[lessonId]` | Màn hình quiz/challenge | 🔴 Cao |
| 5 | **Shop** | `/shop` | Cửa hàng mua hearts bằng points | 🟡 Trung bình |
| 6 | **Quests** | `/quests` | Danh sách nhiệm vụ/câu đố | 🟡 Trung bình |
| 7 | **Leaderboard** | `/leaderboard` | Bảng xếp hạng người dùng | 🟡 Trung bình |
| 8 | **Admin** | `/admin` | Quản lý nội dung (không cần thiết cho mobile) | 🟢 Thấp |

---

## 🏗️ Architecture Android Project

### 📁 Cấu Trúc Thư Mục
```
app/src/main/java/com/duolingo/
├── data/
│   ├── database/
│   │   ├── entities/
│   │   ├── daos/
│   │   └── AppDatabase.kt
│   ├── repository/
│   └── models/
├── domain/
│   ├── usecases/
│   └── models/
├── presentation/
│   ├── screens/
│   │   ├── home/
│   │   ├── courses/
│   │   ├── learn/
│   │   ├── lesson/
│   │   ├── shop/
│   │   ├── quests/
│   │   └── leaderboard/
│   ├── components/
│   ├── viewmodel/
│   └── navigation/
├── di/
└── MainActivity.kt
```

### 🔧 Dependencies Chính
```kotlin
// build.gradle (Module: app)
dependencies {
    // Compose BOM
    implementation platform('androidx.compose:compose-bom:2024.02.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-graphics'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.compose.material3:material3'
    
    // Navigation
    implementation 'androidx.navigation:navigation-compose:2.7.6'
    
    // ViewModel
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0'
    
    // Room Database
    implementation 'androidx.room:room-runtime:2.6.1'
    implementation 'androidx.room:room-ktx:2.6.1'
    kapt 'androidx.room:room-compiler:2.6.1'
    
    // Hilt DI
    implementation 'com.google.dagger:hilt-android:2.48'
    implementation 'androidx.hilt:hilt-navigation-compose:1.1.0'
    kapt 'com.google.dagger:hilt-compiler:2.48'
    
    // Image Loading
    implementation 'io.coil-kt:coil-compose:2.5.0'
    
    // Audio
    implementation 'androidx.media3:media3-exoplayer:1.2.0'
}
```

---

## 📱 Migration Từng Màn Hình

### 1. 🏠 Home/Landing Screen

#### Next.js Version
```tsx
// src/app/(marketing)/page.tsx
export default function Home() {
  return (
    <div className="mx-auto flex w-full max-w-[988px] flex-1 flex-col items-center justify-center gap-2 p-4 lg:flex-row">
      <div className="relative mb-8 h-[240px] w-[240px] lg:mb-0 lg:h-[424px] lg:w-[424px]">
        <Image src="/hero.svg" alt="Hero" fill loading="eager" />
      </div>
      <div className="flex flex-col items-center gap-y-8">
        <h1 className="max-w-[480px] text-center text-xl font-bold text-neutral-600 lg:text-3xl">
          Learn, practise, and master new languages with DuoLingo.
        </h1>
        <div className="flex w-full max-w-[330px] flex-col items-center gap-y-3">
          <SignUpButton>
            <Button size="lg" variant="secondary" className="w-full">
              Get Started
            </Button>
          </SignUpButton>
          <SignInButton>
            <Button size="lg" variant="primaryOutline" className="w-full">
              I already have an account
            </Button>
          </SignInButton>
        </div>
      </div>
    </div>
  );
}
```

#### Android Compose Version
```kotlin
// presentation/screens/home/HomeScreen.kt
@Composable
fun HomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToLearn: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hero Image
        AsyncImage(
            model = R.drawable.hero,
            contentDescription = "Hero",
            modifier = Modifier
                .size(240.dp)
                .padding(bottom = 32.dp),
            contentScale = ContentScale.Fit
        )
        
        // Title
        Text(
            text = "Learn, practise, and master new languages with DuoLingo.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        // Buttons
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is AuthState.SignedOut -> {
                    Button(
                        onClick = onNavigateToRegister,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Get Started")
                    }
                    OutlinedButton(
                        onClick = onNavigateToLogin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("I already have an account")
                    }
                }
                is AuthState.SignedIn -> {
                    Button(
                        onClick = onNavigateToLearn,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue Learning")
                    }
                }
            }
        }
    }
}
```

#### ViewModel
```kotlin
// presentation/viewmodel/HomeViewModel.kt
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    val authState = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )
}
```

---

### 2. 📚 Courses Screen

#### Next.js Version
```tsx
// src/app/(main)/courses/page.tsx
const CoursesPage = async () => {
  const [courses, userProgress] = await Promise.all([
    getCourses(),
    getUserProgress(),
  ]);

  return (
    <div className="mx-auto h-full max-w-[912px] px-3">
      <h1 className="text-2xl font-bold text-neutral-700">Language Courses</h1>
      <List courses={courses} activeCourseId={userProgress?.activeCourseId} />
    </div>
  );
};
```

#### Android Compose Version
```kotlin
// presentation/screens/courses/CoursesScreen.kt
@Composable
fun CoursesScreen(
    onCourseSelected: (Int) -> Unit,
    viewModel: CoursesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCourses()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Language Courses",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        when (uiState) {
            is CoursesUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CoursesUiState.Success -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.courses) { course ->
                        CourseCard(
                            course = course,
                            isActive = course.id == uiState.activeCourseId,
                            onClick = { onCourseSelected(course.id) }
                        )
                    }
                }
            }
            is CoursesUiState.Error -> {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

#### Course Card Component
```kotlin
// presentation/components/CourseCard.kt
@Composable
fun CourseCard(
    course: Course,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = course.imageSrc,
                contentDescription = course.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = course.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (isActive) {
                    Text(
                        text = "Active Course",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
```

#### ViewModel
```kotlin
// presentation/viewmodel/CoursesViewModel.kt
@HiltViewModel
class CoursesViewModel @Inject constructor(
    private val coursesRepository: CoursesRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<CoursesUiState>(CoursesUiState.Loading)
    val uiState: StateFlow<CoursesUiState> = _uiState.asStateFlow()
    
    fun loadCourses() {
        viewModelScope.launch {
            try {
                val courses = coursesRepository.getAllCourses()
                val userProgress = userProgressRepository.getUserProgress()
                
                _uiState.value = CoursesUiState.Success(
                    courses = courses,
                    activeCourseId = userProgress?.activeCourseId
                )
            } catch (e: Exception) {
                _uiState.value = CoursesUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class CoursesUiState {
    object Loading : CoursesUiState()
    data class Success(
        val courses: List<Course>,
        val activeCourseId: Int?
    ) : CoursesUiState()
    data class Error(val message: String) : CoursesUiState()
}
```

---

### 3. 📖 Learn Screen

#### Next.js Version
```tsx
// src/app/(main)/learn/page.tsx
const LearnPage = async () => {
  const [userProgress, units, courseProgress, lessonPercentage, userSubscription] = 
    await Promise.all([
      getUserProgress(),
      getUnits(),
      getCourseProgress(),
      getLessonPercentage(),
      getUserSubscription(),
    ]);

  return (
    <div className="flex flex-row-reverse gap-[48px] px-6">
      <StickyWrapper>
        <UserProgress
          activeCourse={userProgress.activeCourse}
          hearts={userProgress.hearts}
          points={userProgress.points}
          hasActiveSubscription={!!userSubscription?.isActive}
        />
        {!isPro && <Promo />}
        <Quests points={userProgress.points} />
      </StickyWrapper>
      <FeedWrapper>
        <Header title={userProgress.activeCourse.title} />
        {units.map((unit) => (
          <Unit
            key={unit.id}
            id={unit.id}
            order={unit.order}
            description={unit.description}
            title={unit.title}
            lessons={unit.lessons}
            activeLesson={courseProgress?.activeLesson}
            activeLessonPercentage={lessonPercentage}
          />
        ))}
      </FeedWrapper>
    </div>
  );
};
```

#### Android Compose Version
```kotlin
// presentation/screens/learn/LearnScreen.kt
@Composable
fun LearnScreen(
    onNavigateToLesson: (Int) -> Unit,
    viewModel: LearnViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadLearnData()
    }
    
    when (uiState) {
        is LearnUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is LearnUiState.Success -> {
            LearnContent(
                data = uiState.data,
                onNavigateToLesson = onNavigateToLesson
            )
        }
        is LearnUiState.Error -> {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun LearnContent(
    data: LearnData,
    onNavigateToLesson: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = data.userProgress.activeCourse.title,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Units List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(data.units) { unit ->
                    UnitCard(
                        unit = unit,
                        activeLesson = data.courseProgress?.activeLesson,
                        activeLessonPercentage = data.lessonPercentage,
                        onLessonClick = onNavigateToLesson
                    )
                }
            }
        }
        
        // Sidebar
        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
        ) {
            UserProgressCard(
                activeCourse = data.userProgress.activeCourse,
                hearts = data.userProgress.hearts,
                points = data.userProgress.points,
                hasActiveSubscription = data.isPro
            )
            
            if (!data.isPro) {
                PromoCard()
            }
            
            QuestsCard(points = data.userProgress.points)
        }
    }
}
```

#### Unit Card Component
```kotlin
// presentation/components/UnitCard.kt
@Composable
fun UnitCard(
    unit: Unit,
    activeLesson: Lesson?,
    activeLessonPercentage: Float,
    onLessonClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = unit.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = unit.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(unit.lessons) { lesson ->
                    LessonButton(
                        lesson = lesson,
                        isActive = lesson.id == activeLesson?.id,
                        isCompleted = lesson.completed,
                        onClick = { onLessonClick(lesson.id) }
                    )
                }
            }
        }
    }
}
```

---

### 4. 🎯 Lesson Screen

#### Next.js Version
```tsx
// src/app/lesson/page.tsx
const LessonPage = async () => {
  const [lesson, userProgress, userSubscription] = await Promise.all([
    getLesson(),
    getUserProgress(),
    getUserSubscription(),
  ]);

  const initialPercentage = 
    (lesson.challenges.filter(c => c.completed).length / lesson.challenges.length) * 100;

  return (
    <Quiz
      initialLessonId={lesson.id}
      initialLessonChallenges={lesson.challenges}
      initialHearts={userProgress.hearts}
      initialPercentage={initialPercentage}
      userSubscription={userSubscription}
    />
  );
};
```

#### Android Compose Version
```kotlin
// presentation/screens/lesson/LessonScreen.kt
@Composable
fun LessonScreen(
    lessonId: Int,
    onNavigateBack: () -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(lessonId) {
        viewModel.loadLesson(lessonId)
    }
    
    when (uiState) {
        is LessonUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        is LessonUiState.Success -> {
            LessonContent(
                data = uiState.data,
                onNavigateBack = onNavigateBack,
                onAnswerSelected = viewModel::selectAnswer,
                onNextChallenge = viewModel::nextChallenge
            )
        }
        is LessonUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = onNavigateBack) {
                    Text("Go Back")
                }
            }
        }
    }
}

@Composable
fun LessonContent(
    data: LessonData,
    onNavigateBack: () -> Unit,
    onAnswerSelected: (Int) -> Unit,
    onNextChallenge: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        LessonHeader(
            hearts = data.hearts,
            percentage = data.percentage,
            onBackClick = onNavigateBack
        )
        
        // Progress Bar
        LinearProgressIndicator(
            progress = data.percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
        
        // Current Challenge
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            when (data.currentChallenge) {
                is Challenge.Select -> {
                    SelectChallengeCard(
                        challenge = data.currentChallenge,
                        onAnswerSelected = onAnswerSelected
                    )
                }
                is Challenge.Assist -> {
                    AssistChallengeCard(
                        challenge = data.currentChallenge,
                        onAnswerSelected = onAnswerSelected
                    )
                }
            }
        }
        
        // Footer
        LessonFooter(
            currentChallengeIndex = data.currentChallengeIndex,
            totalChallenges = data.totalChallenges,
            onNext = onNextChallenge
        )
    }
}
```

#### Challenge Components
```kotlin
// presentation/components/challenges/SelectChallengeCard.kt
@Composable
fun SelectChallengeCard(
    challenge: Challenge.Select,
    onAnswerSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = challenge.question,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(challenge.options) { option ->
                ChallengeOptionCard(
                    option = option,
                    onClick = { onAnswerSelected(option.id) }
                )
            }
        }
    }
}

@Composable
fun ChallengeOptionCard(
    option: ChallengeOption,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (option.imageSrc != null) {
                AsyncImage(
                    model = option.imageSrc,
                    contentDescription = option.text,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Text(
                text = option.text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
```

---

### 5. 🛍️ Shop Screen

#### Next.js Version
```tsx
// src/app/(main)/shop/page.tsx
const ShopPage = async () => {
  const [userProgress, userSubscription] = await Promise.all([
    getUserProgress(),
    getUserSubscription(),
  ]);

  return (
    <div className="flex flex-row-reverse gap-[48px] px-6">
      <StickyWrapper>
        <UserProgress {...userProgress} />
        {!isPro && <Promo />}
        <Quests points={userProgress.points} />
      </StickyWrapper>
      <FeedWrapper>
        <div className="flex w-full flex-col items-center">
          <Image src="/shop.svg" alt="shop" height={90} width={90} />
          <h1 className="my-6 text-center text-2xl font-bold text-neutral-800">Shop</h1>
          <Items
            hearts={userProgress.hearts}
            points={userProgress.points}
            hasActiveSubscription={isPro}
          />
        </div>
      </FeedWrapper>
    </div>
  );
};
```

#### Android Compose Version
```kotlin
// presentation/screens/shop/ShopScreen.kt
@Composable
fun ShopScreen(
    viewModel: ShopViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadShopData()
    }
    
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = R.drawable.shop,
                contentDescription = "Shop",
                modifier = Modifier
                    .size(90.dp)
                    .padding(bottom = 24.dp)
            )
            
            Text(
                text = "Shop",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "Spend your points on cool stuff.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            when (uiState) {
                is ShopUiState.Success -> {
                    ShopItemsList(
                        items = uiState.items,
                        userPoints = uiState.userProgress.points,
                        onItemPurchase = viewModel::purchaseItem
                    )
                }
                is ShopUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> CircularProgressIndicator()
            }
        }
        
        // Sidebar
        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
        ) {
            UserProgressCard(
                activeCourse = uiState.userProgress?.activeCourse,
                hearts = uiState.userProgress?.hearts ?: 0,
                points = uiState.userProgress?.points ?: 0,
                hasActiveSubscription = uiState.isPro
            )
            
            if (!uiState.isPro) {
                PromoCard()
            }
            
            QuestsCard(points = uiState.userProgress?.points ?: 0)
        }
    }
}

@Composable
fun ShopItemsList(
    items: List<ShopItem>,
    userPoints: Int,
    onItemPurchase: (Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            ShopItemCard(
                item = item,
                userPoints = userPoints,
                onPurchase = { onItemPurchase(item.id) }
            )
        }
    }
}

@Composable
fun ShopItemCard(
    item: ShopItem,
    userPoints: Int,
    onPurchase: () -> Unit
) {
    val canAfford = userPoints >= item.price
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (canAfford) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.imageSrc,
                contentDescription = item.title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${item.price} XP",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (canAfford) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = onPurchase,
                    enabled = canAfford
                ) {
                    Text("Buy")
                }
            }
        }
    }
}
```

---

### 6. 🎯 Quests Screen

#### Next.js Version
```tsx
// src/app/(main)/quests/page.tsx
const QuestsPage = async () => {
  const [userProgress, userSubscription] = await Promise.all([
    getUserProgress(),
    getUserSubscription(),
  ]);

  return (
    <div className="flex flex-row-reverse gap-[48px] px-6">
      <StickyWrapper>
        <UserProgress {...userProgress} />
        {!isPro && <Promo />}
      </StickyWrapper>
      <FeedWrapper>
        <div className="flex w-full flex-col items-center">
          <Image src="/quests.svg" alt="quests" height={90} width={90} />
          <h1 className="my-6 text-center text-2xl font-bold text-neutral-800">Quests</h1>
          {quests.map((quest) => {
            const progress = (userProgress.points / quest.value) * 100;
            return (
              <div key={quest.value} className="flex w-full items-center gap-x-4 border-t-2 p-4">
                <Image src="/points.svg" alt="point" height={60} width={60} />
                <div className="flex w-full flex-col gap-y-2">
                  <p className="text-xl font-bold text-neutral-700">{quest.title}</p>
                  <Progress value={progress} className="h-3" />
                </div>
              </div>
            );
          })}
        </div>
      </FeedWrapper>
    </div>
  );
};
```

#### Android Compose Version
```kotlin
// presentation/screens/quests/QuestsScreen.kt
@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadQuestsData()
    }
    
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = R.drawable.quests,
                contentDescription = "Quests",
                modifier = Modifier
                    .size(90.dp)
                    .padding(bottom = 24.dp)
            )
            
            Text(
                text = "Quests",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "Complete quest by earning points.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            when (uiState) {
                is QuestsUiState.Success -> {
                    QuestsList(
                        quests = uiState.quests,
                        userPoints = uiState.userProgress.points
                    )
                }
                is QuestsUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> CircularProgressIndicator()
            }
        }
        
        // Sidebar
        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
        ) {
            UserProgressCard(
                activeCourse = uiState.userProgress?.activeCourse,
                hearts = uiState.userProgress?.hearts ?: 0,
                points = uiState.userProgress?.points ?: 0,
                hasActiveSubscription = uiState.isPro
            )
            
            if (!uiState.isPro) {
                PromoCard()
            }
        }
    }
}

@Composable
fun QuestsList(
    quests: List<Quest>,
    userPoints: Int
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quests) { quest ->
            QuestCard(
                quest = quest,
                userPoints = userPoints
            )
        }
    }
}

@Composable
fun QuestCard(
    quest: Quest,
    userPoints: Int
) {
    val progress = (userPoints.toFloat() / quest.targetPoints) * 100f
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = R.drawable.points,
                contentDescription = "Points",
                modifier = Modifier.size(60.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                )
                
                Text(
                    text = "$userPoints / ${quest.targetPoints} points",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
```

---

### 7. 🏆 Leaderboard Screen

#### Next.js Version
```tsx
// src/app/(main)/leaderboard/page.tsx
const LeaderBoardPage = async () => {
  const [userProgress, userSubscription, leaderboard] = await Promise.all([
    getUserProgress(),
    getUserSubscription(),
    getTopTenUsers(),
  ]);

  return (
    <div className="flex flex-row-reverse gap-[48px] px-6">
      <StickyWrapper>
        <UserProgress {...userProgress} />
        {!isPro && <Promo />}
        <Quests points={userProgress.points} />
      </StickyWrapper>
      <FeedWrapper>
        <div className="flex w-full flex-col items-center">
          <Image src="/leaderboard.svg" alt="leaderboard" height={90} width={90} />
          <h1 className="my-6 text-center text-2xl font-bold text-neutral-800">Leaderboard</h1>
          <p className="mb-6 text-center text-lg text-muted-foreground">
            See where you stand among other learners in the community.
          </p>
          <Separator className="mb-4 h-0.5 rounded-full" />
          {leaderboard.map((userProgress, index) => (
            <div key={userProgress.userId} className="flex w-full items-center rounded-xl p-2 px-4 hover:bg-gray-200/50">
              <p className="mr-4 font-bold text-lime-700">{index + 1}</p>
              <Avatar className="ml-3 mr-6 h-12 w-12 border bg-green-500">
                <AvatarImage src={userProgress.userImageSrc} />
              </Avatar>
              <p className="flex-1 font-bold text-neutral-800">{userProgress.userName}</p>
              <p className="text-muted-foreground">{userProgress.points} XP</p>
            </div>
          ))}
        </div>
      </FeedWrapper>
    </div>
  );
};
```

#### Android Compose Version
```kotlin
// presentation/screens/leaderboard/LeaderboardScreen.kt
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadLeaderboardData()
    }
    
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Main Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = R.drawable.leaderboard,
                contentDescription = "Leaderboard",
                modifier = Modifier
                    .size(90.dp)
                    .padding(bottom = 24.dp)
            )
            
            Text(
                text = "Leaderboard",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            Text(
                text = "See where you stand among other learners in the community.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            when (uiState) {
                is LeaderboardUiState.Success -> {
                    LeaderboardList(
                        leaderboard = uiState.leaderboard
                    )
                }
                is LeaderboardUiState.Error -> {
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> CircularProgressIndicator()
            }
        }
        
        // Sidebar
        Column(
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
        ) {
            UserProgressCard(
                activeCourse = uiState.userProgress?.activeCourse,
                hearts = uiState.userProgress?.hearts ?: 0,
                points = uiState.userProgress?.points ?: 0,
                hasActiveSubscription = uiState.isPro
            )
            
            if (!uiState.isPro) {
                PromoCard()
            }
            
            QuestsCard(points = uiState.userProgress?.points ?: 0)
        }
    }
}

@Composable
fun LeaderboardList(
    leaderboard: List<UserProgress>
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(leaderboard) { index, userProgress ->
            LeaderboardItem(
                rank = index + 1,
                userProgress = userProgress
            )
        }
    }
}

@Composable
fun LeaderboardItem(
    rank: Int,
    userProgress: UserProgress
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .width(32.dp)
                    .padding(end = 16.dp)
            )
            
            AsyncImage(
                model = userProgress.userImageSrc,
                contentDescription = userProgress.userName,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = userProgress.userName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${userProgress.points} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## 🗺️ Navigation Setup

### Navigation Graph
```kotlin
// presentation/navigation/AppNavigation.kt
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToLearn = { navController.navigate("learn") }
            )
        }
        
        composable("login") {
            LoginScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLearn = { navController.navigate("learn") }
            )
        }
        
        composable("register") {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLearn = { navController.navigate("learn") }
            )
        }
        
        composable("courses") {
            CoursesScreen(
                onCourseSelected = { courseId ->
                    navController.navigate("learn/$courseId")
                }
            )
        }
        
        composable("learn/{courseId}") { backStackEntry ->
            val courseId = backStackEntry.arguments?.getString("courseId")?.toIntOrNull()
            courseId?.let { id ->
                LearnScreen(
                    courseId = id,
                    onNavigateToLesson = { lessonId ->
                        navController.navigate("lesson/$lessonId")
                    },
                    onNavigateToCourses = {
                        navController.popBackStack("courses", false)
                    }
                )
            }
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
        
        composable("shop") {
            ShopScreen()
        }
        
        composable("quests") {
            QuestsScreen()
        }
        
        composable("leaderboard") {
            LeaderboardScreen()
        }
    }
}
```

---

## 🎯 Migration Timeline

### Phase 1: Foundation (Tuần 1-2)
- [ ] Setup Android project với Jetpack Compose
- [ ] Implement Room database schema
- [ ] Setup MVVM architecture với Hilt
- [ ] Implement basic navigation

### Phase 2: Core Screens (Tuần 3-4)
- [ ] **Home Screen** - Authentication flow
- [ ] **Courses Screen** - Course selection
- [ ] **Learn Screen** - Main learning interface
- [ ] **Lesson Screen** - Quiz/challenge system

### Phase 3: Gamification (Tuần 5-6)
- [ ] **Shop Screen** - Points to hearts exchange
- [ ] **Quests Screen** - Achievement system
- [ ] **Leaderboard Screen** - Social features

### Phase 4: Polish & Testing (Tuần 7-8)
- [ ] UI/UX refinement
- [ ] Performance optimization
- [ ] Testing (Unit, Integration, UI)
- [ ] App store preparation

---

## 📋 Checklist Migration

### ✅ Pre-Migration
- [ ] Backup Next.js project
- [ ] Document current features
- [ ] Setup Android development environment
- [ ] Create migration timeline

### ✅ Database Migration
- [ ] Export PostgreSQL data
- [ ] Create Room entities
- [ ] Implement DAOs
- [ ] Setup database migrations
- [ ] Test data integrity

### ✅ UI Components
- [ ] Create reusable Compose components
- [ ] Implement Material Design 3 theme
- [ ] Setup image loading với Coil
- [ ] Implement responsive layouts

### ✅ Business Logic
- [ ] Implement ViewModels
- [ ] Setup Repository pattern
- [ ] Implement use cases
- [ ] Setup dependency injection

### ✅ Testing
- [ ] Unit tests cho ViewModels
- [ ] Integration tests cho Repositories
- [ ] UI tests cho critical flows
- [ ] Performance testing

### ✅ Deployment
- [ ] Setup CI/CD pipeline
- [ ] Create release builds
- [ ] Prepare app store assets
- [ ] Submit to Google Play Store

---

## 🚀 Next Steps

1. **Bắt đầu với Phase 1**: Setup Android project foundation
2. **Implement từng screen theo thứ tự ưu tiên**
3. **Test thoroughly** sau mỗi screen được hoàn thành
4. **Maintain code quality** với proper architecture patterns
5. **Document changes** và update guide này khi cần

---

*Guide này sẽ được cập nhật liên tục trong quá trình migration. Mỗi screen được hoàn thành sẽ có thêm chi tiết implementation và lessons learned.*
