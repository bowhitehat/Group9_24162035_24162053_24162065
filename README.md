# Hệ thống quản lý đề tài sinh viên — Group 9

**TRƯỜNG ĐẠI HỌC CÔNG NGHỆ KỸ THUẬT TP.HCM**

**Khoa Công nghệ Thông tin**

Ứng dụng quản lý quy trình đề tài môn học, NCKH, TLCN và KLTN: tạo đợt, đề xuất và duyệt đề tài, lập nhóm, đăng ký, nộp báo cáo, phân công phản biện, hội đồng, chấm điểm và công bố kết quả.

## Thành viên

| Thành viên | MSSV | Phụ trách |
|---|---:|---|
| Trang Sĩ Hoàng | 24162035 | Nền tảng kỹ thuật, tích hợp, SQL Workbench, tài khoản–phân quyền, bộ môn và đợt đăng ký |
| Vũ Trọng Hưng | 24162053 | Đề tài, nhóm sinh viên, đăng ký đề tài và nộp báo cáo |
| Trần Hào Kiệt | 24162065 | Phản biện, hội đồng, chấm điểm, thông báo, dashboard và triển khai |

## Công nghệ

- Java 21, Spring Boot 3.5, Maven.
- Spring MVC, Thymeleaf, Validation.
- Spring Data JPA, MySQL 8, Flyway.
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
- Repository là lớp duy nhất truy cập dữ liệu.
- Flyway là nguồn sự thật của schema; không sửa migration đã được chia sẻ.
- File người dùng nằm ngoài `static`, theo cấu hình `app.upload-dir`.

Quy tắc tích hợp chi tiết: [docs/shared-contract.md](docs/shared-contract.md).

## Chạy development

### 1. Chuẩn bị MySQL

1. Cài Java 21, Maven 3.9+ và MySQL 8.
2. Mở MySQL Workbench bằng tài khoản quản trị.
3. Mở [sql/mysql-workbench-setup.sql](sql/mysql-workbench-setup.sql), thay mật khẩu mẫu rồi chạy toàn bộ script.
4. Sao chép `.env.example` thành `.env` và chỉ điền bí mật trong `.env`.

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

## Kiểm thử và đóng gói

```bash
mvn test
mvn clean package
```

File JAR được tạo trong `target/`. Profile test dùng H2; trước khi bàn giao phải chạy thêm ứng dụng thật với MySQL để Flyway và Hibernate `validate` toàn bộ schema.

## Docker

```bash
docker compose up --build
```

- Ứng dụng: `http://localhost:8080/login`.
- Health check: `http://localhost:8080/actuator/health`.
- MySQL và thư mục upload dùng Docker volume.

Xem thêm [deployment/README.md](deployment/README.md).

## Tài liệu

- [Hướng dẫn MySQL Workbench](docs/mysql-workbench-guide.md)
- [Data contract dùng chung](docs/shared-contract.md)
- [Thiết kế cơ sở dữ liệu](docs/chapter-4.md)
- [Báo cáo kiểm thử](docs/system-test-report.md)
- [Checklist nộp bài](SUBMISSION_CHECKLIST.md)
