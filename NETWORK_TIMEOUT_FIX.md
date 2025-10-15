# Giải quyết lỗi SocketTimeoutException khi Signup

## 🔍 **Nguyên nhân lỗi:**
```
java.net.SocketTimeoutException: failed to connect to /10.0.2.2 (port 8000) from /192.168.2.8 (port 44650) after 30000ms
```

**Vấn đề**: Android Emulator không thể kết nối đến backend server trên `10.0.2.2:8000`.

## ✅ **Giải pháp đã áp dụng:**

### 1. **Khởi động Korean Learning Backend Server**

```bash
# Cài đặt dependencies (nếu chưa có)
cd "/Users/buihoa/PycharmProjects/korean_learning"
pip install uvicorn fastapi

# Khởi động server với host 0.0.0.0 để accept connections từ mọi IP
python -m uvicorn src.app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 2. **Cập nhật BASE_URL trong Android**

**File**: `app/build.gradle.kts`

```kotlin
// Thay đổi từ:
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8000/api/\"")

// Thành:
buildConfigField("String", "BASE_URL", "\"http://192.168.1.23:8000/api/\"")
```

### 3. **Rebuild Android App**

```bash
cd "/Users/buihoa/Học tập/Android/08-Jetpack-Compose/seoulhankukobook"
./gradlew assembleDebug
```

## 🔧 **Các cách khắc phục khác:**

### **Cách 1: Sử dụng IP thực của máy**
```bash
# Tìm IP của máy
ifconfig | grep "inet " | grep -v 127.0.0.1

# Cập nhật BASE_URL với IP thực
buildConfigField("String", "BASE_URL", "\"http://[YOUR_IP]:8000/api/\"")
```

### **Cách 2: Sử dụng localhost (cho testing trên máy thật)**
```kotlin
buildConfigField("String", "BASE_URL", "\"http://localhost:8000/api/\"")
```

### **Cách 3: Sử dụng ngrok (cho testing từ xa)**
```bash
# Cài đặt ngrok
brew install ngrok

# Expose local server
ngrok http 8000

# Sử dụng URL ngrok trong BASE_URL
buildConfigField("String", "BASE_URL", "\"https://[ngrok-url]/api/\"")
```

## 🧪 **Kiểm tra kết nối:**

### **Test từ terminal:**
```bash
# Test server có chạy không
curl -v http://192.168.1.23:8000/api/v1/register

# Kết quả mong đợi: HTTP 405 Method Not Allowed (vì dùng GET thay vì POST)
```

### **Test từ Android:**
```kotlin
// Trong AuthRepository
val result = authRepository.signUp(
    name = "Test User",
    username = "testuser",
    email = "test@example.com", 
    password = "password123"
)

result.fold(
    onSuccess = {
        println("✅ Signup thành công!")
    },
    onFailure = { error ->
        println("❌ Signup thất bại: ${error.message}")
    }
)
```

## 📱 **Cấu hình cho các môi trường khác nhau:**

### **Development (Local)**
```kotlin
buildConfigField("String", "BASE_URL", "\"http://192.168.1.23:8000/api/\"")
```

### **Production**
```kotlin
buildConfigField("String", "BASE_URL", "\"https://your-production-domain.com/api/\"")
```

### **Staging**
```kotlin
buildConfigField("String", "BASE_URL", "\"https://staging.your-domain.com/api/\"")
```

## 🚨 **Lưu ý quan trọng:**

1. **Firewall**: Đảm bảo port 8000 không bị firewall chặn
2. **Network**: Android Emulator và máy host phải cùng network
3. **Server Status**: Luôn kiểm tra server có chạy trước khi test
4. **IP Changes**: IP có thể thay đổi khi restart router, cần cập nhật lại

## 🔍 **Debug Steps:**

1. **Kiểm tra server**: `curl http://[IP]:8000/api/v1/register`
2. **Kiểm tra network**: `ping [IP]` từ Android Emulator
3. **Kiểm tra logs**: Xem logs của server và Android app
4. **Kiểm tra BASE_URL**: Đảm bảo BuildConfig.BASE_URL đúng

## ✅ **Kết quả:**

Sau khi áp dụng giải pháp:
- ✅ Backend server chạy trên `192.168.1.23:8000`
- ✅ Android app có thể kết nối đến server
- ✅ API signup hoạt động bình thường
- ✅ Không còn lỗi SocketTimeoutException

**Lưu ý**: Nếu IP thay đổi, cần cập nhật lại BASE_URL và rebuild app.







