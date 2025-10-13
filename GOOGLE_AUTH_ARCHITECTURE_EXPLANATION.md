# Google Authentication Architecture Explanation

## 🏗️ Kiến trúc tổng thể

### Luồng Google Sign-In hoàn chỉnh:

```
[Android App] ←→ [Google APIs] ←→ [FastAPI Backend] ←→ [Database]
     ↓              ↓                    ↓               ↓
  User UI    Google ID Token      JWT Token        User Data
```

## 🔄 Luồng chi tiết

### 1. **Initial Login Flow**

```
1. User clicks "Sign in with Google"
   ↓
2. GoogleSignInClient.getSignInIntent()
   ↓
3. User selects Google account
   ↓
4. GoogleSignInAccount received (with ID token)
   ↓
5. Android app extracts ID token
   ↓
6. POST /api/v1/auth/google { "token": "<id_token>" }
   ↓
7. Backend verifies ID token with Google APIs
   ↓
8. Backend creates/updates user in database
   ↓
9. Backend generates JWT token (7 days)
   ↓
10. Backend returns JWT token
    ↓
11. Android app saves JWT token to DataStore
    ↓
12. Navigate to CoursesScreen
```

### 2. **Auto Login Flow**

```
1. App launches → SplashActivity
   ↓
2. Check JWT token in DataStore
   ↓
3. If JWT token exists and valid:
   - Navigate to CoursesScreen
   ↓
4. If no JWT token:
   - Navigate to HomeScreen (login required)
```

### 3. **Logout Flow**

```
1. User clicks "Logout"
   ↓
2. GoogleSignInClient.signOut() (clears Google session)
   ↓
3. Clear JWT token from DataStore
   ↓
4. Navigate to LoginScreen
```

## 🤔 Tại sao cần backend xác minh lại ID token?

### **1. Security Reasons**

#### **Google ID Token chỉ là "proof of identity":**
- ✅ Xác minh user là ai (email, name, picture)
- ✅ Xác minh token được Google cấp phát
- ❌ **KHÔNG** chứa thông tin về quyền hạn trong app
- ❌ **KHÔNG** thể kiểm soát thời hạn từ phía app

#### **JWT Token của backend chứa:**
- ✅ User permissions/roles trong app
- ✅ App-specific data (course progress, achievements)
- ✅ Thời hạn do app kiểm soát
- ✅ Có thể revoke bất cứ lúc nào

### **2. Data Management**

#### **Google ID Token:**
```json
{
  "iss": "https://accounts.google.com",
  "aud": "your-client-id",
  "sub": "google-user-id",
  "email": "user@gmail.com",
  "name": "John Doe",
  "picture": "https://..."
}
```

#### **Backend JWT Token:**
```json
{
  "sub": "username-in-app",
  "exp": 1234567890,
  "iat": 1234567890,
  "role": "student",
  "course_progress": {...},
  "achievements": [...]
}
```

### **3. Session Control**

#### **Google Session:**
- Được Google kiểm soát hoàn toàn
- App không thể revoke
- User có thể logout từ Google → ảnh hưởng tất cả apps

#### **App Session (JWT):**
- App kiểm soát hoàn toàn
- Có thể revoke ngay lập tức
- Không ảnh hưởng bởi Google logout
- Có thể set thời hạn khác nhau cho từng app

## 🔐 Tại sao không dùng Google token trực tiếp?

### **1. Limited Control**
```kotlin
// Google token - app không kiểm soát được
if (googleToken.isValid()) {
    // Nhưng app không biết user có quyền gì trong app
    // Không biết user đã học course nào
    // Không biết user có premium không
}
```

### **2. No App-Specific Data**
```kotlin
// JWT token - app kiểm soát hoàn toàn
if (jwtToken.isValid()) {
    val userRole = jwtToken.getRole() // "student", "premium", "admin"
    val courseProgress = jwtToken.getCourseProgress()
    val achievements = jwtToken.getAchievements()
    // App biết chính xác user có thể làm gì
}
```

### **3. Security Vulnerabilities**
- Google token có thể bị replay attack
- Không có rate limiting
- Không có audit trail
- Không thể track suspicious activities

## 🔄 Cách đồng bộ session giữa Google và Backend

### **1. Initial Sync (Login)**
```kotlin
// Android
val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
val idToken = googleAccount?.idToken

// Gửi đến backend
val request = GoogleSignInRequest(token = idToken)
val response = apiService.signInWithGoogle(request)

// Lưu JWT token
userPreferencesManager.saveUserData(
    accessToken = response.token, // JWT từ backend
    // ... other data
)
```

### **2. Session Validation**
```kotlin
// Mỗi API call
val jwtToken = userPreferencesManager.accessToken
val request = SomeApiRequest()
request.addHeader("Authorization", "Bearer $jwtToken")
```

### **3. Session Refresh**
```kotlin
// Khi JWT token sắp hết hạn
if (isTokenExpiringSoon(jwtToken)) {
    // Có thể refresh token hoặc yêu cầu login lại
    val newToken = refreshToken()
    userPreferencesManager.updateAccessToken(newToken)
}
```

### **4. Logout Sync**
```kotlin
// Android logout
googleSignInClient.signOut() // Clear Google session
userPreferencesManager.clearUserData() // Clear JWT token

// Backend có thể blacklist JWT token
// (tùy thuộc implementation)
```

## 📊 So sánh các phương pháp

| Aspect | Google Token Only | JWT Token Only | Hybrid (Current) |
|--------|------------------|----------------|------------------|
| **Security** | ⚠️ Medium | ✅ High | ✅ Highest |
| **Control** | ❌ Limited | ✅ Full | ✅ Full |
| **Performance** | ✅ Fast | ⚠️ Medium | ⚠️ Medium |
| **User Experience** | ✅ Seamless | ❌ Complex | ✅ Seamless |
| **Data Integration** | ❌ Poor | ✅ Good | ✅ Excellent |
| **Session Management** | ❌ Poor | ✅ Good | ✅ Excellent |

## 🎯 Lợi ích của kiến trúc hiện tại

### **1. Security**
- ✅ Double verification (Google + Backend)
- ✅ App-controlled session management
- ✅ Audit trail và monitoring
- ✅ Rate limiting và abuse prevention

### **2. User Experience**
- ✅ Seamless login với Google
- ✅ Auto-login với JWT token
- ✅ Consistent experience across app
- ✅ Fast subsequent API calls

### **3. Data Management**
- ✅ User data được lưu trong database
- ✅ Course progress tracking
- ✅ Gamification features
- ✅ Social features (friends, leaderboard)

### **4. Scalability**
- ✅ JWT tokens stateless
- ✅ Easy to scale horizontally
- ✅ Microservices ready
- ✅ Mobile/web compatibility

## 🚨 Lưu ý quan trọng

### **1. Token Expiration**
```kotlin
// Backend JWT token có thời hạn 7 ngày
// Nếu hết hạn, user cần login lại
if (jwtToken.isExpired()) {
    // Redirect to login screen
    navigateToLogin()
}
```

### **2. Network Errors**
```kotlin
// Xử lý khi backend không available
try {
    val response = apiService.signInWithGoogle(request)
} catch (e: NetworkException) {
    // Fallback: có thể cache Google session
    // và thử lại khi có network
}
```

### **3. Security Best Practices**
- ✅ Luôn sử dụng HTTPS
- ✅ Validate JWT signature
- ✅ Check token expiration
- ✅ Implement proper logout
- ✅ Monitor suspicious activities

## 🎉 Kết luận

Kiến trúc hiện tại kết hợp **Google authentication** với **JWT-based session management** mang lại:

1. **Security cao nhất** - Double verification
2. **User experience tốt nhất** - Seamless login
3. **Control hoàn toàn** - App quản lý session
4. **Data integration** - Full app features
5. **Scalability** - Ready for growth

Đây là pattern **industry standard** được sử dụng bởi các app lớn như Spotify, Netflix, Uber, etc.
