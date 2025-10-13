# Google Sign-In Setup Guide

## 📋 Tổng quan

Chức năng Google Sign-In đã được tích hợp thành công vào ứng dụng Seoul Hankuko Book với package name `com.seoulhankuko.app`. 

## 🏗️ Cấu trúc đã triển khai

### 1. Data Models
- `GoogleSignInModels.kt`: Chứa các model cho request/response API
- `GoogleSignInResult`: Sealed class cho các trạng thái đăng nhập

### 2. Repository Layer
- `GoogleSignInRepository.kt`: Xử lý logic Google Sign-In và gọi API backend
- `UserPreferencesManager.kt`: Quản lý lưu trữ user data bằng DataStore

### 3. ViewModel Layer
- `GoogleSignInViewModel.kt`: Quản lý state và business logic cho UI

### 4. UI Components
- `GoogleSignInButton.kt`: Component nút đăng nhập Google với Material 3 design
- `SocialSignInSection.kt`: Section chứa cả Google và Facebook sign-in buttons

### 5. Integration
- `LoginScreen.kt`: Đã tích hợp Google Sign-In button
- `ApiService.kt`: Thêm endpoint `POST /auth/google`

## ⚙️ Setup cần thiết

### 1. Google Cloud Console Setup

**Bước 1: Tạo OAuth 2.0 Client ID**
1. Truy cập [Google Cloud Console](https://console.cloud.google.com/)
2. Chọn project của bạn
3. Đi đến **APIs & Services** > **Credentials**
4. Click **Create Credentials** > **OAuth 2.0 Client ID**
5. Chọn **Web application**
6. Thêm authorized redirect URIs nếu cần
7. Copy **Client ID** được tạo

**Bước 2: Cập nhật code**
1. Mở file `GoogleSignInRepository.kt`
2. Thay thế `"YOUR_WEB_CLIENT_ID"` bằng Client ID thực từ bước 1:

```kotlin
fun getGoogleSignInClient(context: android.content.Context): GoogleSignInClient {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("YOUR_ACTUAL_WEB_CLIENT_ID_HERE") // Thay thế ở đây
        .requestEmail()
        .requestProfile()
        .build()
    
    return GoogleSignIn.getClient(context, gso)
}
```

### 2. Backend API Setup

**Endpoint cần implement:**
```
POST /api/v1/auth/google
Content-Type: application/json

{
  "id_token": "google_id_token_here"
}
```

**Response format:**
```json
{
  "access_token": "your_access_token",
  "refresh_token": "your_refresh_token",
  "user": {
    "id": "user_id",
    "email": "user@example.com",
    "name": "User Name",
    "avatar_url": "https://example.com/avatar.jpg",
    "is_premium": false
  },
  "message": "Login successful"
}
```

### 3. Google Services Configuration

**Cập nhật google-services.json:**
File hiện tại thiếu OAuth client configuration. Cần thêm vào `oauth_client` section:

```json
{
  "oauth_client": [
    {
      "client_id": "YOUR_WEB_CLIENT_ID",
      "client_type": 3
    }
  ]
}
```

## 🚀 Cách sử dụng

### 1. Trong LoginScreen
Google Sign-In button đã được tích hợp tự động. Khi user click:
1. Mở Google Sign-In flow
2. Lấy thông tin user (email, name, idToken)
3. Gửi idToken đến backend `/auth/google`
4. Nếu thành công → lưu user data và điều hướng
5. Nếu thất bại → hiển thị error message

### 2. Trong code khác
```kotlin
// Inject GoogleSignInViewModel
@Composable
fun YourScreen(
    viewModel: GoogleSignInViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userData by viewModel.userData.collectAsState()
    
    // Check if user is signed in
    if (isLoggedIn) {
        // User is signed in, show content
    } else {
        // User is not signed in, show login
    }
}
```

## 🔧 Dependencies đã thêm

```kotlin
// Google Sign-In
implementation("com.google.android.gms:play-services-auth:21.0.0")

// DataStore for user preferences
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

## 🎨 UI Features

- **Material 3 Design**: Sử dụng Material 3 components
- **Loading States**: Hiển thị loading indicator khi đang xử lý
- **Error Handling**: Hiển thị error messages với Snackbar/Toast
- **Responsive**: Tự động disable/enable buttons dựa trên state
- **Accessibility**: Support accessibility features

## 🧪 Testing

### Unit Tests
- `GoogleSignInRepositoryTest`: Test repository logic
- `GoogleSignInViewModelTest`: Test ViewModel state management
- `UserPreferencesManagerTest`: Test DataStore operations

### Integration Tests
- `GoogleSignInIntegrationTest`: Test end-to-end flow
- `ApiServiceTest`: Test API calls

## 🐛 Troubleshooting

### Common Issues

**1. "Google Sign-In failed: 10"**
- Kiểm tra SHA-1 fingerprint trong Firebase Console
- Đảm bảo package name khớp với google-services.json

**2. "Invalid client ID"**
- Kiểm tra Client ID trong GoogleSignInRepository.kt
- Đảm bảo đã tạo OAuth 2.0 Client ID cho Web application

**3. "Network error"**
- Kiểm tra kết nối internet
- Kiểm tra backend API endpoint
- Kiểm tra BASE_URL trong build.gradle.kts

**4. "User data not saved"**
- Kiểm tra DataStore permissions
- Kiểm tra UserPreferencesManager implementation

## 📱 Demo Flow

1. **User mở app** → Thấy LoginScreen
2. **User click "Đăng nhập bằng Google"** → Mở Google Sign-In dialog
3. **User chọn account** → Google trả về idToken
4. **App gửi idToken đến backend** → Backend xác thực và trả về user data
5. **App lưu user data** → Điều hướng đến màn hình chính
6. **User đã đăng nhập** → Có thể sử dụng app với tài khoản Google

## 🔒 Security Notes

- **ID Token Validation**: Backend phải validate Google ID token
- **Token Storage**: Access token được lưu an toàn trong DataStore
- **Network Security**: Sử dụng HTTPS cho tất cả API calls
- **Error Messages**: Không expose sensitive information trong error messages

## 📚 References

- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Material 3 Components](https://developer.android.com/jetpack/compose/design-systems/material3)
- [DataStore Guide](https://developer.android.com/topic/libraries/architecture/datastore)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)

---

**Lưu ý**: Để sử dụng đầy đủ chức năng, cần setup Google Cloud Console và implement backend API endpoint `/auth/google`.

