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

## Email và xuất kết quả

- Đặt `APP_BASE_URL` thành URL public của ứng dụng (Docker local: `http://localhost:8080`). Link phân công phản biện không còn hard-code localhost.
- SMTP dùng `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH`, `MAIL_STARTTLS`, `MAIL_FROM`; không commit `.env` hay mật khẩu.
- Email phân công/công bố chỉ được đưa vào hàng đợi sau khi transaction commit; rollback không gửi. Công bố chỉ gửi cho các thành viên của nhóm được duyệt, không gửi lại khi bấm công bố lần hai.
- Queue/SMTP lỗi được ghi log, không hủy kết quả đã lưu. Đây chưa phải durable outbox: mất tiến trình hoặc lỗi SMTP có thể mất email, chưa có retry tự động. Không gọi email đã gửi thành công khi chỉ mới công bố kết quả.
- Excel giữ điểm dạng số, PDF nhúng Noto Sans để giữ dấu tiếng Việt. Font kèm giấy phép SIL OFL tại `src/main/resources/fonts/OFL.txt`, nguồn: https://github.com/notofonts/noto-fonts.
- Sinh viên vào `Kết quả của tôi`, chọn đợt đã đăng ký. Đợt chưa công bố chỉ hiển thị thông báo chờ, không lộ điểm.

## Kiểm thử và đóng gói bản tích hợp 06/10/2026

- `mvn -o -B clean package`: 171 lượt test Java đạt, không lỗi/skip; có test HTTP server thật cho đăng nhập và quyền.
- `node --test src/test/js/period-form.test.cjs`: 24 test đạt.
- Hồi quy kiểm tra sửa thông báo không double-escape, trang điểm không lazy-load lỗi, chặn xem đợt/nhóm khác, email sau commit/rollback, gửi tới mọi thành viên và không lặp, queue đầy không gây 500, Excel số và PDF tiếng Việt.
- Script đóng gói đã chạy trên bản sao kiểm thử: đúng thư mục gốc, có tên `BaoCao_DoAn.docx`, không có `sql`, `.env`, `.git`, `target`, `uploads`. Không coi ZIP kiểm thử là bài nộp chính thức.
- Với Word đã được duyệt nhưng đổi tên, chạy `./tools/package_submission.ps1 -ReportFile 'ten-ban-bao-cao-da-duyet.docx'`. Script đổi tên bản sao trong ZIP; không sửa file Word gốc.
- Chưa kiểm thử lại MySQL thật, chưa gửi SMTP production, chưa có URL hosting. Không ghi các bước này đã hoàn thành.

## Hosting miễn phí

Cần kiểm tra tại thời điểm deploy: hỗ trợ Java 21 hoặc Docker, MySQL, biến môi trường, disk cho upload.

Không hard-code secret. Nếu máy chủ không có persistent disk, file upload có thể mất khi restart; nên dùng object storage.

## Trạng thái production

URL production: **chưa có**. Việc triển khai đang bị chặn vì workspace chưa có tài khoản/credential của nhà cung cấp hosting; báo cáo và checklist không sử dụng URL giả.

Khi nhóm cung cấp tài khoản, cần kiểm tra lại health check, kết nối database, đăng nhập, phân quyền, static resources và upload. Nếu hosting không có persistent disk, upload local chỉ phù hợp để demo vì file có thể mất sau restart hoặc redeploy.
