# Hướng dẫn triển khai

Module thành viên 3 chuẩn bị Docker, profile `prod` và health check `/actuator/health`.

## Yêu cầu

- Java 21
- Maven 3.9+
- MySQL 8 (khi chạy ngoài Docker)
- Docker + Docker Compose nếu dùng container

## Chạy bằng Docker Compose

1. Sao chép `.env.example` thành `.env` và đổi mật khẩu.
2. `docker compose up --build`
3. Mở `http://localhost:8080/login`
4. Health check: `http://localhost:8080/actuator/health`

## Chạy JAR

```bash
mvn -DskipTests package
java -jar target/topic-management-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

Biến môi trường bắt buộc: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.

## Hosting miễn phí

Cần kiểm tra tại thời điểm deploy: hỗ trợ Java 21 hoặc Docker, MySQL, biến môi trường, disk cho upload.

Không hard-code secret. Nếu máy chủ không có persistent disk, file upload có thể mất khi restart; nên dùng object storage.

URL production: chưa gắn hosting công khai trong commit này vì bước đăng nhập nhà cung cấp cần tài khoản của nhóm.
