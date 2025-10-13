# Auto Login Implementation

## Tổng quan

Tính năng **tự động đăng nhập bằng Google** đã được triển khai thành công trong ứng dụng Seoul Hankuko Book. Khi người dùng mở app lần sau, nếu tài khoản Google vẫn còn session thì app sẽ tự động điều hướng đến CoursesScreen mà không cần chọn tài khoản lại.

## Cơ chế hoạt động

### 1. Flow Auto Login

```
App Launch → SplashActivity → Kiểm tra Google Sign-In session → Điều hướng
```

#### Chi tiết từng bước:

1. **App Launch**: SplashActivity được khởi động đầu tiên (đã cấu hình trong AndroidManifest.xml)

2. **SplashActivity**: 
   - Hiển thị logo và animation trong 1.5 giây
   - Sử dụng `GoogleSignIn.getLastSignedInAccount(context)` để kiểm tra session
   - Điều hướng đến MainActivity

3. **MainActivity**:
   - Sử dụng `AppNavigationWithAutoLogin` để kiểm tra trạng thái đăng nhập
   - Nếu đã đăng nhập → điều hướng đến "courses"
   - Nếu chưa đăng nhập → điều hướng đến "home"

### 2. Tại sao đặt logic kiểm tra login trong SplashScreen?

- **User Experience**: Người dùng thấy logo app trong khi hệ thống kiểm tra session
- **Performance**: Không cần load toàn bộ UI trước khi biết cần điều hướng đến đâu
- **Security**: Kiểm tra session ngay từ đầu, tránh hiển thị nội dung không phù hợp
- **Standard Practice**: Đây là pattern phổ biến trong các ứng dụng mobile

### 3. Cách đảm bảo logout hoạt động đúng

#### Khi người dùng logout:

1. **GoogleSignInViewModel.signOut()** được gọi
2. **GoogleSignInClient.signOut()** - xóa session Google
3. **UserPreferencesManager.clearUserData()** - xóa dữ liệu local
4. **Navigation** - điều hướng về LoginScreen

#### Khi mở app lần sau:

- `GoogleSignIn.getLastSignedInAccount()` sẽ trả về `null`
- App sẽ điều hướng đến HomeScreen thay vì CoursesScreen
- Người dùng cần đăng nhập lại

## Các file đã được cập nhật

### 1. SplashActivity.kt
- Thêm logic kiểm tra Google Sign-In session
- Sử dụng `GoogleSignIn.getLastSignedInAccount(context)`
- Điều hướng phù hợp dựa trên trạng thái session

### 2. MainActivity.kt
- Thêm `AppNavigationWithAutoLogin` composable
- Kiểm tra trạng thái đăng nhập từ GoogleSignInViewModel
- Điều hướng đến "courses" hoặc "home" dựa trên trạng thái

### 3. AppNavigation.kt
- Cập nhật để nhận `initialDestination` parameter
- Bỏ màn hình Register (redirect về Login)
- Thêm callback logout cho CoursesScreen

### 4. CoursesScreen.kt
- Thêm nút Logout
- Sử dụng GoogleSignInViewModel để thực hiện logout
- Điều hướng về LoginScreen sau khi logout

## Cấu hình Google Sign-In

GoogleSignInOptions đã được cấu hình với:
- `DEFAULT_SIGN_IN` - cấu hình cơ bản
- `requestEmail()` - yêu cầu email
- `requestProfile()` - yêu cầu thông tin profile

Client ID được lấy từ `google-services.json` (đã có sẵn trong project).

## Testing Flow

### Test Case 1: Đăng nhập lần đầu
1. Mở app → SplashActivity → HomeScreen
2. Đăng nhập Google → CoursesScreen
3. Đóng app

### Test Case 2: Auto Login
1. Mở app → SplashActivity → CoursesScreen (tự động)
2. Không cần chọn tài khoản lại

### Test Case 3: Logout và mở lại
1. Từ CoursesScreen → nhấn Logout → LoginScreen
2. Đóng app
3. Mở app → SplashActivity → HomeScreen (cần đăng nhập lại)

## Lưu ý quan trọng

1. **Session Management**: Google Sign-In session được quản lý tự động bởi Google Play Services
2. **Data Persistence**: UserPreferencesManager lưu trữ thông tin người dùng local
3. **Error Handling**: Có xử lý lỗi trong GoogleSignInRepository và ViewModel
4. **Performance**: Sử dụng StateFlow và LaunchedEffect để tối ưu hiệu suất

## Kết luận

Tính năng auto login đã được triển khai thành công với:
- ✅ Kiểm tra session trong SplashActivity
- ✅ Điều hướng tự động đến CoursesScreen nếu đã đăng nhập
- ✅ Xử lý logout và quay về LoginScreen
- ✅ Bỏ màn hình Register
- ✅ Không có lỗi linter
- ✅ Tuân thủ best practices của Android/Compose
