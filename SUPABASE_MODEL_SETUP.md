# Checklist: Setup Supabase Storage cho TFLite Model

## Mục đích
Upload file `hangul_stroke_model.tflite` (~42MB) lên Supabase Storage thay vì commit vào GitHub để tránh làm repo quá lớn.

---

## ✅ Checklist

### 1. Setup Supabase Storage

- [ ] Đăng nhập vào Supabase Dashboard: https://app.supabase.com
- [ ] Chọn project của bạn (hoặc tạo project mới nếu chưa có)
- [ ] Vào **Storage** trong sidebar
- [ ] Tạo bucket mới:
  - [ ] Click **"New bucket"**
  - [ ] Tên bucket: `models`
  - [ ] Chọn **Public bucket** (để có thể download không cần auth)
  - [ ] Click **"Create bucket"**

### 2. Upload Model File

- [ ] Vào bucket `models` vừa tạo
- [ ] Click **"Upload file"**
- [ ] Chọn file: `app/src/main/assets/hangul_stroke_model.tflite`
- [ ] Đợi upload hoàn tất (có thể mất vài phút vì file ~42MB)
- [ ] Verify file đã upload thành công

### 3. Lấy Public URL

- [ ] Click vào file `hangul_stroke_model.tflite` trong bucket
- [ ] Copy **Public URL** (format: `https://[project-ref].supabase.co/storage/v1/object/public/models/hangul_stroke_model.tflite`)
- [ ] Lưu URL này để cập nhật vào code

### 4. Cập nhật Code

- [ ] Mở file `app/build.gradle.kts`
- [ ] Tìm dòng: `buildConfigField("String", "SUPABASE_MODEL_URL", ...)`
- [ ] Thay `"https://your-project.supabase.co/..."` bằng URL thực tế từ bước 3
- [ ] Sync Gradle project

### 5. Cấu hình DI Module (Optional)

- [ ] Mở file `app/src/main/java/com/seoulhankuko/app/di/HangulClassifierModule.kt`
- [ ] Kiểm tra `USE_SUPABASE = true` (mặc định đã là `true`)
- [ ] Nếu muốn dùng assets thay vì Supabase, đổi thành `false`

### 6. Test App

- [ ] Build và chạy app
- [ ] Mở màn hình "Luyện viết Hangul"
- [ ] Kiểm tra Logcat:
  - [ ] Tìm log: `"Attempting to load model from Supabase..."`
  - [ ] Tìm log: `"Model not found locally, downloading from Supabase..."`
  - [ ] Tìm log: `"Model downloaded successfully: X bytes"`
  - [ ] Tìm log: `"HangulTFLiteClassifier created successfully"`
- [ ] Vẽ một ký tự và nhấn "Kiểm tra"
- [ ] Verify model hoạt động đúng (nhận diện được ký tự)

### 7. Cleanup (Optional)

- [ ] Xóa file `hangul_stroke_model.tflite` khỏi `app/src/main/assets/` (nếu muốn)
- [ ] Thêm vào `.gitignore`:
  ```
  app/src/main/assets/hangul_stroke_model.tflite
  ```
- [ ] Commit và push code (không có file model nữa)

---

## 📝 Notes

### Cách hoạt động:

1. **Lần đầu mở app:**
   - App sẽ download model từ Supabase
   - Lưu vào `context.filesDir/hangul_stroke_model.tflite`
   - Sử dụng model đã download

2. **Các lần sau:**
   - App kiểm tra file đã tồn tại trong `filesDir`
   - Nếu có, sử dụng file cached (không download lại)
   - Nếu không có hoặc file bị lỗi, download lại từ Supabase

3. **Fallback:**
   - Nếu download từ Supabase thất bại
   - App sẽ tự động fallback về assets (nếu file còn trong assets)
   - Đảm bảo app vẫn hoạt động ngay cả khi không có internet

### Troubleshooting:

**Lỗi: "Failed to download model"**
- Kiểm tra URL trong `build.gradle.kts` có đúng không
- Kiểm tra bucket có public không
- Kiểm tra internet connection
- Xem Logcat để biết lỗi chi tiết

**Model không hoạt động:**
- Kiểm tra file đã download đúng size chưa (~42MB)
- Xóa file cached: `context.filesDir/hangul_stroke_model.tflite` và thử lại
- Kiểm tra logs trong Logcat

**Muốn force download lại:**
- Xóa app data hoặc uninstall/reinstall app
- Hoặc xóa file: `context.filesDir/hangul_stroke_model.tflite` trong code

---

## 🔗 Links hữu ích

- Supabase Storage Docs: https://supabase.com/docs/guides/storage
- Supabase Dashboard: https://app.supabase.com
- TensorFlow Lite Android: https://www.tensorflow.org/lite/android

---

## ✅ Hoàn thành

Sau khi hoàn thành tất cả các bước trên, app sẽ:
- ✅ Download model từ Supabase khi cần
- ✅ Cache model locally để không cần download lại
- ✅ Fallback về assets nếu Supabase không available
- ✅ Không cần commit file model lớn vào GitHub

