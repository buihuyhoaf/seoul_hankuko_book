# Google Client ID Fix - Lỗi Google Sign-in failed: 10

## 🔍 **Vấn đề:**
```
Google Sign-in failed: 10
```

Lỗi này xảy ra khi có sự không khớp giữa client ID trong Android app và backend.

## ✅ **Giải pháp:**

### 1. **Đã cập nhật Android App**
```kotlin
// File: GoogleSignInRepository.kt
val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
    .requestEmail()
    .requestProfile()
    .requestIdToken("461063240681-81vk4ofnni0lru1vdmnmtvlrbd0k9hpt.apps.googleusercontent.com") // ✅ Web client ID đúng
    .build()
```

### 2. **Cần cập nhật Backend Environment**

Tạo file `.env` trong thư mục `korean_learning/src/`:

```env
# Google Authentication
GOOGLE_CLIENT_ID=461063240681-81vk4ofnni0lru1vdmnmtvlrbd0k9hpt.apps.googleusercontent.com

# Database Configuration
POSTGRES_SERVER=localhost
POSTGRES_PORT=5432
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
POSTGRES_DB=korean_learning

# Security Configuration
SECRET_KEY=your-secret-key-here-change-in-production
ALGORITHM=HS256
ACCESS_TOKEN_EXPIRE_MINUTES=10080
REFRESH_TOKEN_EXPIRE_DAYS=7

# Redis Configuration
REDIS_CACHE_HOST=localhost
REDIS_CACHE_PORT=6379
REDIS_QUEUE_HOST=localhost
REDIS_QUEUE_PORT=6379
REDIS_RATE_LIMIT_HOST=localhost
REDIS_RATE_LIMIT_PORT=6379

# Admin Configuration
ADMIN_USERNAME=admin
ADMIN_PASSWORD=admin123

# App Configuration
APP_NAME=Korean Learning App
APP_DESCRIPTION=Korean Language Learning Platform
APP_VERSION=1.0.0

# Environment
ENVIRONMENT=development
```

### 3. **Khởi động Backend**
```bash
cd korean_learning
python -m uvicorn src.app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 4. **Test Google Sign-In**
```bash
# Test API endpoint
curl -X POST "http://192.168.1.23:8000/api/v1/auth/google" \
  -H "Content-Type: application/json" \
  -d '{"token": "YOUR_GOOGLE_ID_TOKEN"}'
```

## 🔧 **Các bước kiểm tra:**

### 1. **Kiểm tra Google Console**
- Đảm bảo Web client ID: `461063240681-81vk4ofnni0lru1vdmnmtvlrbd0k9hpt.apps.googleusercontent.com`
- Đảm bảo Android app được thêm vào project
- Đảm bảo SHA-1 fingerprint đúng

### 2. **Kiểm tra google-services.json**
- File phải có đúng package name: `com.seoulhankuko.app`
- File phải có đúng project ID

### 3. **Kiểm tra Network**
```bash
# Test backend có chạy không
curl http://192.168.1.23:8000/api/v1/auth/google

# Expected: HTTP 405 Method Not Allowed (vì dùng GET thay vì POST)
```

## 🚨 **Lỗi Google Sign-in failed: 10 có thể do:**

1. **Sai Web client ID** ✅ (Đã sửa)
2. **Backend không chạy** 
3. **Network connection issues**
4. **Google Console configuration sai**
5. **SHA-1 fingerprint không đúng**

## 🎯 **Next Steps:**

1. ✅ Cập nhật Android app với đúng Web client ID
2. ⚠️ Tạo file `.env` trong backend
3. ⚠️ Khởi động backend server
4. ⚠️ Test Google Sign-In

## 📱 **Testing:**

1. **Clean và rebuild Android app**
2. **Khởi động backend server**
3. **Test Google Sign-In**
4. **Kiểm tra logs nếu vẫn lỗi**

---

**Lưu ý**: Đảm bảo backend server đang chạy trên `192.168.1.23:8000` trước khi test.





