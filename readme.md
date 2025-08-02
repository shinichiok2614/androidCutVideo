
# Tích hợp thư viện mobile-ffmpeg-full-4.4.LTS vào Android Studio

Hướng dẫn này giúp bạn tích hợp thư viện `mobile-ffmpeg-full-4.4.LTS.aar` thủ công vào dự án Android Studio (offline).

---

## 🔗 Tài nguyên

- Trang thư viện chính thức: [mobile-ffmpeg-full 4.4.LTS](https://nexus.web.cern.ch/nexus/content/groups/public/com/arthenica/mobile-ffmpeg-full/4.4.LTS/)
- Tài liệu Android Library: [developer.android.com](https://developer.android.com/studio/projects/android-library?hl=vi#kts)

---

## 📥 Bước 1: Tải thư viện `.aar`

1. Truy cập trang:  
   https://nexus.web.cern.ch/nexus/content/groups/public/com/arthenica/mobile-ffmpeg-full/4.4.LTS/
2. Tải file: `mobile-ffmpeg-full-4.4.LTS.aar`

---

## 📁 Bước 2: Thêm vào thư mục `libs`

1. Chuyển Android Studio sang chế độ **Project** (góc trên trái).
2. Di chuyển file `mobile-ffmpeg-full-4.4.LTS.aar` vào thư mục:
   ```
   app/libs/
   ```
3. Nếu chưa có thư mục `libs`, hãy tạo mới thư mục đó.

---

## 🧩 Bước 3: Thêm dependency trong Project Structure

1. Vào `File > Project Structure > Dependencies`
2. Chọn **All Modules**, chọn module `:app`
3. Nhấn nút `+` → chọn **Jar/AAR Dependency** (có thể tìm kiếm online (nếu có) nếu chọn Library dependency: ví dụ tìm exoplayer > search, chọn đúng phiên bản)
4. Chọn file:
   ```
   libs/mobile-ffmpeg-full-4.4.LTS.aar
   ```
5. Nhấn OK.

---

## 🔧 Bước 4: Kiểm tra `build.gradle (Module: app)`

Mở file `app/build.gradle` ở chế độ xem **Android** và kiểm tra trong khối `dependencies` có dòng sau:

```gradle
implementation files('libs/mobile-ffmpeg-full-4.4.LTS.aar')
```

Nếu chưa có, thêm dòng đó vào thủ công.

---

## 🔄 Bước 5: Sync dự án

- Nhấn **Sync Now** hoặc vào `File > Sync Project with Gradle Files`.

---

## ✅ Thành công

- Nếu dòng sau có trong `build.gradle (Module: app)`:
  ```
  implementation files('libs/mobile-ffmpeg-full-4.4.LTS.aar')
  ```
  Thì bạn đã tích hợp thành công.

---

## 💡 Sử dụng

Import thư viện trong Java:

```java
import com.arthenica.mobileffmpeg.FFmpeg;
```

Thực thi lệnh FFmpeg:

```java
FFmpeg.execute("-i input.mp4 -ss 00:00:02 -t 00:00:05 -c copy output.mp4");
```

---

## 📌 Ghi chú

- Android 13+ cần cấp quyền truy cập bộ nhớ để làm việc với tệp video.
