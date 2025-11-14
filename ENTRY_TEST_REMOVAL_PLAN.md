# Kế hoạch loại bỏ Entry Test Feature

## 📋 Tổng quan
Kế hoạch đơn giản: **Xóa toàn bộ màn hình và API liên quan, KHÔNG động đến database**.

---

## 🎯 Nguyên tắc
- ✅ **XÓA:** UI screens, ViewModels, Repositories, API endpoints, Navigation routes
- ✅ **SỬA:** Remove references, cleanup code
- ❌ **KHÔNG ĐỘNG:** Database tables/columns (giữ nguyên data)

---

## 📦 Files cần XÓA HOÀN TOÀN

### Android App

#### 1. UI Screens
```
app/src/main/java/com/seoulhankuko/app/presentation/screens/
  ❌ EntryTestScreen.kt
  ❌ EntryTestResultScreen.kt
```

#### 2. Components
```
app/src/main/java/com/seoulhankuko/app/presentation/components/
  ❌ EntryTestReminderDialog.kt
```

#### 3. ViewModels
```
app/src/main/java/com/seoulhankuko/app/presentation/viewmodel/
  ❌ EntryTestViewModel.kt
  ❌ EntryTestFlowViewModel.kt
```

#### 4. Repository
```
app/src/main/java/com/seoulhankuko/app/data/repository/
  ❌ EntryTestRepository.kt
```

### Backend API

#### 1. API Routes
```
src/app/api/v1/
  ❌ entry_test.py
```

#### 2. Schemas
```
src/app/schemas/
  ❌ entry_test.py
```

---

## 🔧 Files cần SỬA (không xóa)

### Android App

#### 1. Navigation - `AppNavigation.kt`
**Cần xóa:**
- Route "entry-test" (dòng ~666-684)
- Route "entry-test-result" (dòng ~686-703)
- Import `EntryTestScreen`
- Import `EntryTestResultScreen`

**Cách làm:**
```kotlin
// XÓA toàn bộ composable này:
composable("entry-test") { ... }

composable("entry-test-result") { ... }
```

#### 2. FirstScreen - `FirstScreen.kt`
**Cần xóa:**
- Parameter `onNavigateToEntryTest: () -> Unit = {}`
- Callback trong `FirstScreen()` composable
- Logic navigate đến entry test (nếu có)

**Cách làm:**
```kotlin
// XÓA parameter:
fun FirstScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToLearn: (courseId: Int) -> Unit,
    // ❌ onNavigateToEntryTest: () -> Unit = {},  // XÓA DÒNG NÀY
    onNavigateToGuestMode: () -> Unit = {},
    ...
)

// XÓA callback trong AppNavigation:
FirstScreen(
    ...
    // ❌ onNavigateToEntryTest = { navController.navigate("entry-test") },  // XÓA
    ...
)
```

#### 3. MainActivity - `MainActivity.kt`
**Cần xóa:**
- Import `EntryTestFlowViewModel`
- Parameter `entryTestViewModel: EntryTestFlowViewModel = hiltViewModel()`
- Logic sync entry test data (dòng ~109-138)
- Logic check `hasCompletedEntryTest()`
- Logic reset popup dismissal

**Cách làm:**
```kotlin
// XÓA import:
// ❌ import com.seoulhankuko.app.presentation.viewmodel.EntryTestFlowViewModel

// XÓA parameter:
fun AppNavigationWithAutoLogin(
    ...
    // ❌ entryTestViewModel: EntryTestFlowViewModel = hiltViewModel(),  // XÓA
    ...
)

// XÓA toàn bộ block này:
// ❌ try {
// ❌     val syncSuccess = entryTestViewModel.syncUserDataFromBackend()
// ❌     if (syncSuccess) {
// ❌         delay(100)
// ❌     }
// ❌     val hasCompletedEntryTest = entryTestViewModel.hasCompletedEntryTest()
// ❌     if (!hasCompletedEntryTest) {
// ❌         entryTestViewModel.resetEntryTestPopupDismissal()
// ❌     }
// ❌     initialDestination = "courses"
// ❌ } catch (e: Exception) { ... }
```

#### 4. ApiService - `ApiService.kt`
**Cần xóa:**
- Method `getEntryTestQuestions()`
- Method `submitEntryTest()`
- Method `getEntryTestResult()`

**Cách làm:**
```kotlin
// XÓA 3 methods này:
// ❌ @GET("v1/entry-test/")
// ❌ suspend fun getEntryTestQuestions(...): Response<EntryTestResponse>

// ❌ @POST("v1/entry-test/submit")
// ❌ suspend fun submitEntryTest(...): Response<EntryTestSubmissionResponse>

// ❌ @GET("v1/entry-test/result")
// ❌ suspend fun getEntryTestResult(...): Response<EntryTestResultResponse>
```

#### 5. API Models - `KoreanLearningApiModels.kt`
**Cần xóa:**
- `EntryTestResponse`
- `EntryTestQuestionResponse`
- `EntryTestQuestionOptionResponse`
- `EntryTestSubmissionRequest`
- `EntryTestAnswerRequest`
- `EntryTestSubmissionResponse`
- `EntryTestResultResponse`

**Cách làm:**
```kotlin
// XÓA tất cả data classes/models liên quan đến EntryTest
```

#### 6. UserPreferencesManager - `UserPreferencesManager.kt`
**Cần xóa:**
- Keys: `HAS_COMPLETED_ENTRY_TEST_KEY`, `ENTRY_TEST_SCORE_KEY`, `ENTRY_TEST_*_OFFLINE_KEY`, `ENTRY_TEST_NEEDS_SYNC_KEY`, `ENTRY_TEST_POPUP_DISMISSED_KEY`
- Methods: `saveEntryTestResult()`, `hasCompletedEntryTest()`, `getEntryTestScore()`, `hasCompletedEntryTestFlow`, `saveEntryTestResultOffline()`, `hasCompletedEntryTestOffline()`, `getOfflineEntryTestData()`, `entryTestNeedsSync()`, `markEntryTestSynced()`, `needsEntryTest()`, `shouldShowEntryTestPopup()`, `dismissEntryTestPopup()`, `resetEntryTestPopupDismissal()`
- Logic clear entry test data trong `clearUserData()`

**Cách làm:**
- Tìm và xóa tất cả keys liên quan
- Tìm và xóa tất cả methods liên quan
- Xóa logic trong `clearUserData()` nếu có

#### 7. AuthRepository - `AuthRepository.kt`
**Cần xóa:**
- Logic lưu `hasCompletedEntryTest` và `entryTestScore` khi fetch user
- References đến entry test fields

**Cách làm:**
```kotlin
// Tìm và XÓA các dòng:
// ❌ hasCompletedEntryTest = user.hasCompletedEntryTest,
// ❌ entryTestScore = user.entryTestScore
// ❌ userPreferencesManager.saveEntryTestResult(...)
```

#### 8. GoogleSignInRepository - `GoogleSignInRepository.kt`
**Cần xóa:**
- Dependency `EntryTestRepository` (nếu có)
- Logic sync offline entry test trong `signInWithGoogle()`
- Logic save entry test result

**Cách làm:**
```kotlin
// XÓA dependency:
// ❌ private val entryTestRepository: EntryTestRepository,

// XÓA logic sync:
// ❌ val syncSuccess = entryTestRepository.syncOfflineEntryTestToServer()
```

#### 9. ModernHomeScreen (nếu có)
**Cần xóa:**
- Logic hiển thị entry test popup
- Logic check `shouldShowEntryTestPopup()`

#### 10. DI Modules (nếu có)
**Cần xóa:**
- Provide `EntryTestRepository` trong Hilt modules

### Backend API

#### 1. API Router - `src/app/api/v1/__init__.py`
**Cần xóa:**
```python
# ❌ from .entry_test import router as entry_test_router
# ❌ router.include_router(entry_test_router)
```

#### 2. Admin Views - `src/app/admin/views.py`
**Cần xóa:**
- Import entry test models
- Entry Test CRUD views
- Entry Test Question CRUD views
- Entry Test Question Option CRUD views
- Entry Test Result (Score Range) CRUD views
- User Entry Test History views

**Cách làm:**
```python
# XÓA imports:
# ❌ from ..models.entry_test import EntryTest, EntryTestQuestion, ...
# ❌ from ..schemas.entry_test import EntryTestCreate, ...

# XÓA admin views:
# ❌ admin.add_view(EntryTestAdmin(...))
# ❌ admin.add_view(EntryTestQuestionAdmin(...))
# ...
```

#### 3. Models Init - `src/app/models/__init__.py`
**Cần xóa:**
```python
# ❌ from .entry_test import EntryTest, EntryTestQuestion, ...
```

#### 4. User Schema - `src/app/schemas/user.py` (Optional)
**Có thể:**
- Giữ fields nhưng ignore khi serialize (nếu cần backward compatibility)
- Hoặc xóa fields nếu không cần

---

## ✅ Checklist thực hiện

### Phase 1: Xóa Files (15 phút)
- [ ] Xóa `EntryTestScreen.kt`
- [ ] Xóa `EntryTestResultScreen.kt`
- [ ] Xóa `EntryTestReminderDialog.kt`
- [ ] Xóa `EntryTestViewModel.kt`
- [ ] Xóa `EntryTestFlowViewModel.kt`
- [ ] Xóa `EntryTestRepository.kt`
- [ ] Xóa `entry_test.py` (backend)
- [ ] Xóa `entry_test.py` schemas (backend)

### Phase 2: Sửa Navigation & MainActivity (30 phút)
- [ ] Xóa routes trong `AppNavigation.kt`
- [ ] Xóa imports trong `AppNavigation.kt`
- [ ] Xóa parameter `onNavigateToEntryTest` trong `FirstScreen.kt`
- [ ] Xóa callback trong `AppNavigation.kt` khi gọi `FirstScreen`
- [ ] Xóa logic entry test trong `MainActivity.kt`
- [ ] Xóa import `EntryTestFlowViewModel` trong `MainActivity.kt`

### Phase 3: Sửa API Layer (30 phút)
- [ ] Xóa 3 endpoints trong `ApiService.kt`
- [ ] Xóa tất cả entry test models trong `KoreanLearningApiModels.kt`
- [ ] Xóa entry test keys trong `UserPreferencesManager.kt`
- [ ] Xóa entry test methods trong `UserPreferencesManager.kt`
- [ ] Xóa logic entry test trong `AuthRepository.kt`
- [ ] Xóa logic entry test trong `GoogleSignInRepository.kt`

### Phase 4: Sửa Backend (15 phút)
- [ ] Xóa include router trong `src/app/api/v1/__init__.py`
- [ ] Xóa admin views trong `src/app/admin/views.py`
- [ ] Xóa imports trong `src/app/models/__init__.py` (nếu có)

### Phase 5: Cleanup (15 phút)
- [ ] Xóa unused imports (IDE sẽ báo)
- [ ] Xóa unused strings trong `strings.xml` (nếu có)
- [ ] Xóa DI provides cho `EntryTestRepository` (nếu có)
- [ ] Build project để check errors

### Phase 6: Testing (30 phút)
- [ ] Test app startup - không crash
- [ ] Test login flow - hoạt động bình thường
- [ ] Test guest mode - hoạt động bình thường
- [ ] Test navigation - không có broken links
- [ ] Test user data sync - không bị lỗi
- [ ] Test course selection - user có thể chọn course

---

## ⚠️ Rủi ro và Giải pháp

### 🔴 Rủi ro thấp (vì không động database)

#### 1. App crash với old local data
- **Nguyên nhân:** User có entry test data trong SharedPreferences
- **Giải pháp:** 
  - Clear preferences khi logout (đã có trong `clearUserData()`)
  - Hoặc ignore old data (app vẫn chạy, chỉ không dùng)

#### 2. Broken navigation
- **Nguyên nhân:** Deep links hoặc code cũ navigate đến entry test
- **Giải pháp:** 
  - Xóa tất cả routes
  - Test navigation flows

#### 3. Compile errors
- **Nguyên nhân:** Còn references đến deleted files
- **Giải pháp:** 
  - IDE sẽ báo lỗi ngay
  - Fix từng lỗi một

### 🟢 Rủi ro rất thấp

#### 4. Unused code
- **Giải pháp:** IDE tools + linter sẽ báo

#### 5. String resources
- **Giải pháp:** Review `strings.xml`, xóa unused

---

## 📝 Lưu ý quan trọng

### Database
- ✅ **KHÔNG XÓA** database tables/columns
- ✅ **KHÔNG XÓA** migration files
- ✅ **KHÔNG TẠO** migration mới để drop tables
- ✅ Data vẫn giữ nguyên trong database

### Backward Compatibility
- Nếu có nhiều version app, có thể giữ API một thời gian (deprecated)
- Hoặc force update app version

### User Experience
- Users vẫn có thể chọn course manually
- Có thể set default course cho new users (nếu cần)

---

## 📅 Timeline ước tính

- **Phase 1:** 15 phút
- **Phase 2:** 30 phút
- **Phase 3:** 30 phút
- **Phase 4:** 15 phút
- **Phase 5:** 15 phút
- **Phase 6:** 30 phút

**Tổng cộng:** ~2.5 giờ

---

## 🔄 Rollback Plan

Nếu cần rollback:
1. Revert commits từ git
2. Restore deleted files từ git history
3. Database không bị ảnh hưởng (vì không động)

---

## 🚀 Bắt đầu thực hiện

Sau khi review plan này, có thể bắt đầu thực hiện theo checklist trên.
