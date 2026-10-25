# Hệ thống quản lý đề tài sinh viên — Group 9

**TRƯỜNG ĐẠI HỌC SƯ PHẠM KỸ THUẬT TP.HCM**

**Khoa Công nghệ Thông tin**

Ứng dụng quản lý quy trình đề tài môn học, NCKH, TLCN và KLTN: tạo đợt, đề xuất và duyệt đề tài, lập nhóm, đăng ký, nộp báo cáo, phân công phản biện, hội đồng, chấm điểm và công bố kết quả.

## Thành viên

| Thành viên | MSSV | Phụ trách |
|---|---:|---|
| Trang Sĩ Hoàng | 24162035 | Nền tảng kỹ thuật, tích hợp, CSDL/JPA relationship mapping, tài khoản–phân quyền, bộ môn và đợt đăng ký |
| Vũ Trọng Hưng | 24162053 | Đề tài, nhóm sinh viên, đăng ký đề tài và nộp báo cáo |
| Trần Hào Kiệt | 24162065 | Phản biện, hội đồng, chấm điểm, thông báo, dashboard và triển khai |

## Công nghệ

- Java 21, Spring Boot 3.5, Maven.
- Spring MVC, Thymeleaf, Validation.
- Spring Data JPA, Hibernate ORM và MySQL 8.
- Spring Security, BCrypt, CSRF và method security.
- Spring AOP và transaction management.
- JUnit 5, MockMvc, H2 cho test.
- Docker và Docker Compose cho môi trường triển khai.

## Kiến trúc

Luồng phụ thuộc thống nhất:

```text
HTTP request → Controller → Service → Repository → MySQL
                         ↘ Thymeleaf Model/View
```

- Controller nhận/validate input, gọi service và chọn view.
- Luật nghiệp vụ và transaction đặt tại service.
- Repository dùng `JpaRepository`, derived query, JPQL và relationship mapping; không dùng native SQL/script tạo bảng thủ công.
- Hibernate tạo/cập nhật schema từ các entity bằng `spring.jpa.hibernate.ddl-auto=update`.
- File người dùng nằm ngoài `static`: local theo `app.upload-dir`, cloud dùng bucket Supabase Storage riêng tư; tải file luôn kiểm tra quyền ở backend.

Quy tắc tích hợp chi tiết: [docs/shared-contract.md](docs/shared-contract.md).

## Chạy development

### 1. Chuẩn bị MySQL

1. Cài Java 21, Maven 3.9+ và MySQL 8.
2. Mở MySQL Workbench bằng tài khoản quản trị.
3. Trong MySQL Workbench, tạo schema rỗng tên `group9_topic_management` bằng giao diện **Create Schema**; không chạy script SQL.
4. Sao chép `.env.example` thành `.env`, điền tài khoản MySQL có quyền trên schema đó. Hibernate sẽ tạo bảng từ entity khi ứng dụng khởi động.

Ví dụ:

```properties
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:mysql://127.0.0.1:3306/group9_topic_management?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useSSL=false
DB_USERNAME=group9_app
DB_PASSWORD=your_local_password
DEMO_PASSWORD=Password@123
UPLOAD_DIR=./uploads
PORT=8090
```

Không commit `.env`, mật khẩu thật, `target/` hoặc `uploads/`.

### 2. Chạy ứng dụng

```bash
mvn spring-boot:run
```

Mở `http://localhost:8090/login`.

Khi `DEMO_PASSWORD=Password@123`, dữ liệu development tạo các tài khoản mẫu:

| Tài khoản | Vai trò | Mật khẩu |
|---|---|---|
| `admin` | ADMIN | Giá trị `DEMO_PASSWORD` |
| `faculty` | FACULTY_MANAGER | Giá trị `DEMO_PASSWORD` |
| `lecturer1` | LECTURER | Giá trị `DEMO_PASSWORD` |
| `student1` | STUDENT | Giá trị `DEMO_PASSWORD` |

Initializer chỉ tạo tài khoản còn thiếu. Đổi `DEMO_PASSWORD` không tự đổi mật khẩu của tài khoản đã tồn tại; dùng chức năng quản lý tài khoản nếu cần đặt lại. Không dùng mật khẩu demo cho tài khoản hoặc dịch vụ thật.

## Kiểm thử và đóng gói

```bash
mvn -B -Dspring.config.import= -Dapp.mail.enabled=false -Dapp.demo-password= -Dapp.storage.provider=local -Dspring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true clean verify
node --test src/test/js/period-form.test.cjs src/test/js/date-form.test.cjs
```

File JAR được tạo trong `target/`. Profile test dùng H2 và tạo schema trực tiếp từ entity. Kiểm thử HTTP/MySQL phải dùng database QA riêng, không chạy test xóa dữ liệu trên database local đang demo hoặc Aiven. Node.js chỉ cần để test JavaScript, không cần để chạy ứng dụng Java.

### Rà soát kiến trúc và hiệu năng ngày 21/10/2026

Bản rà soát mới đạt 266 test Java (28 regression tests mới), 35 test JavaScript,
16 test transaction/concurrency trên MySQL riêng, 325 kiểm tra HTTP và 41 kiểm tra
trình duyệt. Đã sửa phân trang/lọc trên RAM, truy vấn lặp và các lỗi cập nhật đồng
thời; giữ nguyên phạm vi quyền xem và dữ liệu demo. Xem [chi tiết bản vá, giới hạn
kiểm thử và cách chạy MySQL QA an toàn](docs/verification-20261021.md).

## Bản demo trên Internet

[Đăng nhập Group 9](https://group9-topic-management-production.up.railway.app/login).

- Railway chạy ứng dụng Spring Boot; Aiven lưu MySQL; Supabase Storage lưu báo cáo riêng tư; Brevo HTTPS API gửi email.
- Tài khoản demo: `admin`, `faculty`, `lecturer1`, `student1`; mật khẩu chung `Password@123`. Đây không phải mật khẩu database/API key.
- Railway đang dùng trial/credit: kiểm tra hạn mức và URL trước ngày báo cáo; không cam kết hosting hoạt động cố định 30 ngày.
- MySQL local và Aiven là hai database độc lập. MySQL Workbench chỉ là công cụ kết nối tới database được chọn, không tự đồng bộ hai nơi.
- Không đưa `.env`, mật khẩu MySQL, khóa Supabase hoặc Brevo vào GitHub/ZIP.

## Luồng demo và các điều kiện nghiệp vụ

1. `faculty` tạo đợt và mở `LECTURER_REGISTRATION`. Giảng viên chỉ đề xuất trong khoảng Giảng viên đăng ký từ/đến **và** đúng trạng thái này; chuyển trạng thái không bỏ qua thời hạn.
2. Giảng viên đề xuất → gửi duyệt; khoa duyệt và công bố đề tài.
3. Khoa mở `STUDENT_REGISTRATION`; sinh viên lập nhóm 3–5 người, trưởng nhóm đăng ký trong khoảng thời gian sinh viên, khoa duyệt đăng ký.
4. Khoa chuyển đúng giai đoạn thực hiện/chấm; trưởng nhóm nộp báo cáo trước hạn; khoa phân công phản biện cho đề tài đã duyệt. Người nhận nhiệm vụ nhận email nếu cấu hình gửi thư đã bật.
5. Tạo hội đồng nháp, thêm 3–5 giảng viên (đúng một Chủ tịch, một Thư ký), chọn ít nhất một đề tài cùng đợt rồi kích hoạt. Với KLTN, ngày báo cáo phải trùng ngày hội đồng của đợt.
6. Từng phản biện/thành viên hội đồng đăng nhập tài khoản được phân công, chấm đủ tiêu chí bắt buộc và bấm **Nộp điểm**, không chỉ **Lưu nháp**.
7. `faculty` vào **Kết quả chấm điểm → Chi tiết**, khóa tất cả phiếu hiện tại rồi bấm **Tính điểm**. Chủ tịch đăng nhập để **Xác nhận**, sau đó `faculty` **Công bố**. Sinh viên đúng nhóm/đợt chỉ thấy điểm đã công bố.
8. Chỉ **Hoàn tất** hội đồng sau khi tất cả thành viên nộp đủ điểm cho từng đề tài. Hội đồng đã có phiếu chấm không được hủy; đề tài đã có phiếu hội đồng không được chuyển sang hội đồng khác để tránh ghi đè lịch sử. Hội đồng cũ hoàn tất nhưng chưa có phiếu vẫn có thể phân công lại.

Điểm tổng kết là trung bình các phiếu phản biện còn hiệu lực và các phiếu của hội đồng hiện tại; mỗi phiếu lấy trung bình tiêu chí bắt buộc đang hoạt động. Phiếu hội đồng cũ/phân công đã hủy không tham gia tính điểm. Danh sách kết quả và Excel/PDF chỉ có một dòng cho mỗi đề tài. Nộp báo cáo đồng thời được tuần tự hóa số phiên bản; giao dịch rollback dọn riêng file vừa tải lên (có ghi cảnh báo nếu kho lưu trữ không cho dọn).

Một giảng viên không được vừa phản biện vừa tham gia hội đồng chấm cùng đề tài. Hệ thống chặn cả hai thứ tự phân công và các request đồng thời; vẫn cho phép hai vai trò trên các đề tài khác nhau. Dữ liệu cũ bị trùng sẽ có cảnh báo và bị chặn chấm/tính/xác nhận/công bố, không tự xóa phiếu. Chi tiết: [quy tắc và kiểm thử](docs/grading-role-exclusivity-20261025.md).

## Email khi chạy localhost

Mặc định `MAIL_ENABLED=false`, không gửi email thật. Có thể chọn một trong hai cấu hình trong `.env`, rồi **khởi động lại ứng dụng**:

- SMTP local: `MAIL_PROVIDER=smtp`, `MAIL_ENABLED=true`; điền `MAIL_HOST`, `MAIL_PORT`, tài khoản/STARTTLS theo máy chủ SMTP hoặc sandbox bạn sử dụng.
- Brevo HTTPS: `MAIL_PROVIDER=brevo`, `MAIL_ENABLED=true`, `MAIL_SMTP_HEALTH_ENABLED=false`; điền `BREVO_API_KEY`, `MAIL_FROM` đã xác minh, `APP_BASE_URL=http://localhost:8090`.

Để thử thư phân công: đặt email thật của bạn cho một tài khoản giảng viên kiểm thử, dùng đề tài/đợt demo đủ điều kiện, rồi phân công phản biện với hạn nộp hợp lệ. Kiểm tra Inbox/Spam và nhật ký Transactional của Brevo. Không gửi API key vào chat hoặc lưu trong mã nguồn. Xem [hướng dẫn Brevo](deployment/DEPLOY_EMAIL_BREVO.txt).

## Docker

```bash
docker compose up --build
```

- Ứng dụng: `http://localhost:8080/login`.
- Health check: `http://localhost:8080/actuator/health`.
- MySQL và thư mục upload dùng Docker volume.

Xem thêm [deployment/README.md](deployment/README.md).

## Tài liệu

- [Yêu cầu, ma trận quyền và phân công đến lớp](docs/system-requirements.md)
- [Từ điển dữ liệu JPA](docs/data-dictionary.md)
- Sơ đồ hiện tại và mô tả đủ 21 bảng: xem báo cáo Word và folder `diagrams/` trong ZIP nộp bài.
- [Contract EmailService/SMTP](docs/mail-integration.md), [Brevo API trên cloud](deployment/DEPLOY_EMAIL_BREVO.txt)
- [Chương 1–2](docs/chapter-1-2.md)
- [Hướng dẫn MySQL Workbench](docs/mysql-workbench-guide.md)
- [Data contract dùng chung](docs/shared-contract.md)
- [Thiết kế cơ sở dữ liệu](docs/chapter-4.md)
- [Báo cáo kiểm thử](docs/system-test-report.md)
- [Checklist nộp bài](SUBMISSION_CHECKLIST.md)

## Bài nộp cập nhật ngày 11/10/2026

- Bản sửa cuối: `mvn -B verify` đạt 238 test Java, 35 test JavaScript; kiểm thử HTTP trên MySQL 8.0.46 riêng đạt 325/325 và kiểm tra trình duyệt 41 lượt không lỗi JavaScript. `clean verify` đã đạt trước ca hồi quy cuối, sau đó `verify` build lại toàn bộ phần thay đổi; không tuyên bố đã thử tải lớn hoặc mọi tính năng trên cloud.
- Chỉ nộp `Group09_24162035_24162053_24162065.zip`. Giải nén, mở `source-code/` (có `pom.xml`) để chạy/build; xem `README.txt` và `TAI_KHOAN_DEMO.txt` ở thư mục gốc.
- Báo cáo chính: `Group9_24162035_24162053_24162065.docx`; báo cáo kết quả dự án: `Group9_baocaokq_duan.docx`.
- Folder `diagrams/` trong ZIP chỉ giữ một ảnh ERD tổng thể đúng báo cáo, mô hình Workbench và bốn use-case; không thay đổi các hình trong Word.
- `source-code/deployment/` chứa hướng dẫn Docker/Railway/Brevo. `docs/` là tài liệu phát triển trên repository, cố ý không đóng gói trùng với báo cáo Word; các liên kết `docs/` phía trên dùng trên GitHub.
- Có kiểm thử hồi quy cho phạm vi phiếu hiện tại, chống ghi đè phiếu lịch sử, điều kiện kích hoạt/hoàn tất hội đồng, kết quả không trùng và nộp báo cáo đồng thời/rollback. SMTP/Brevo/Supabase được test bằng máy chủ giả lập, không gửi thư hoặc tải file thật khi chạy bộ test.
