# Kiến trúc Luồng Xử Lý (Flow Architecture) - Module 4

*Tài liệu mô tả ĐẦY ĐỦ các luồng nghiệp vụ chuẩn cho Hệ thống Phân quyền, Giáo trình Lớp Học, Bài Tập và Gamification.*

---

## 1. Luồng Quản trị Quyền & Tổ chức Khóa học (Admin Role)
Quy trình Quản trị viên khởi tạo môi trường học tập.

```mermaid
sequenceDiagram
    actor Admin
    participant BE as Backend System
    participant DB as Database

    %% User Management
    Admin->>BE: Tạo tài khoản Nhân viên
    BE->>DB: Thêm User với Role.TEACHER
    
    %% Course Management
    Admin->>BE: Tạo Khóa Học mới (Course)
    BE->>DB: Lưu thông tin Course
    
    Admin->>BE: Phân công Giáo viên phụ trách
    BE->>DB: Gắn Teacher_ID vào Course
    
    Admin->>BE: Thêm Sinh viên vào lớp
    BE->>DB: Lưu quan hệ Course_Student
    BE-->>Admin: Xác nhận thiết lập xong
```

---

## 2. Luồng Thiết lập Giáo trình & Tài liệu (Teacher Syllabus)
Giáo viên tải tài liệu và cấu hình chương trình giảng dạy.

```mermaid
sequenceDiagram
    actor Teacher
    participant BE as Backend System
    participant Cloud as Cloud Storage (S3/Cloudinary)
    participant DB as Database

    Teacher->>BE: Upload File Bài giảng (Slide/MP4)
    BE->>Cloud: Streaming luồng Upload
    Cloud-->>BE: Trả về Secure URL File
    BE->>DB: Lưu TeachingMaterial
    
    Teacher->>BE: Cấu hình Lộ trình Tuần học (Syllabus)
    BE->>DB: Link TeachingMaterial vào SyllabusItem tương ứng
    BE-->>Teacher: Giáo trình đã sẵn sàng
```

---

## 3. Vòng đời Xử lý Bài Tập Về Nhà (Assignment Lifecycle)
Tương tác 2 chiều giữa Giáo viên (Giao bài & Chấm) và Học viên (Làm bài & Nộp).

```mermaid
stateDiagram-v2
    [*] --> CREATED: Giáo viên tạo Assignment (Đặt Deadline, Đính kèm tài liệu)
    
    state Student_Phase {
        CREATED --> PENDING: Sinh viên nhận được bài
        PENDING --> IN_PROGRESS: Sinh viên mở giao diện làm bài
        IN_PROGRESS --> SUBMITTED: Sinh viên nộp bài (Upload file/Text)
    }
    
    state Teacher_Phase {
        SUBMITTED --> GRADING: Giáo viên mở list bài chưa chấm
        GRADING --> GRADED: Giáo viên cho Điểm & Nhận xét Text/Audio
    }
    
    GRADED --> Notified: Push Notification tới App Sinh viên
    Notified --> [*]
```

---

## 4. Động cơ Gamification Trung Tâm (Global XP & Streak Engine)
Kiến trúc Micro-service/Event-driven xử lý điểm XP và Chuỗi ngày học (Streak) độc lập nhưng kết nối mọi tính năng.

```mermaid
sequenceDiagram
    participant AnyModule as Nguồn Sự kiện (Vocab, MiniGame, Speaking, Assignment)
    participant GameEngine as Gamification Service
    participant DB as Database
    participant Client as Mobile/Web App

    AnyModule->>GameEngine: Event: Hoàn thành nhiệm vụ (Student_ID, Action_Type, Score)
    
    %% Xử lý Điểm Kinh nghiệm
    GameEngine->>GameEngine: Map Action_Type với Bảng Điểm XP cấu hình
    GameEngine->>DB: UPDATE student_stats SET total_xp += base_xp + bonus
    
    %% Xử lý Chuỗi Ngày Học (Streak)
    GameEngine->>DB: Lấy timestamp tương tác cuối cùng (Last Active Date)
    
    alt Nếu là Tương tác Ngày Mới (Sau 0h)
        GameEngine->>GameEngine: Increment Streak (+1)
        GameEngine->>DB: Lưu số ngày Streak mới
        alt Nếu đạt Cột mốc Thưởng (ví dụ: 7 ngày, 30 ngày)
            GameEngine->>GameEngine: Cộng thêm cực lớn XP Thưởng (+500 XP)
        end
    else Cùng ngày
        GameEngine->>GameEngine: Bỏ qua (Chỉ cộng XP ở trên, không tăng Streak)
    else Ngắt quãng thời gian (> 24h)
        GameEngine->>GameEngine: Đứt chuỗi -> Reset Streak = 1
    end
    
    %% Trả Data Về
    GameEngine-->>AnyModule: Object Result (Added_XP, Is_Streak_Updated, Current_Streak)
    AnyModule-->>Client: Trả về chung trong Response của API
    Client->>Client: Cập nhật Global Store & Chạy Hiệu ứng Tung Hoa/Kinh Nghiệm
```
