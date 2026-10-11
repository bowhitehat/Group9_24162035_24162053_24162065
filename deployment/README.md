# Hướng dẫn triển khai

Phương án đang dùng (11/10/2026): **Railway + Aiven MySQL + Supabase Storage + Brevo HTTPS API**.
Demo: [Group 9](https://group9-topic-management-production.up.railway.app/login).
Xem [Railway](DEPLOY_RAILWAY.txt) và [Brevo](DEPLOY_EMAIL_BREVO.txt).
Railway dùng trial/credit, không cam kết thời gian hoạt động cố định; kiểm tra hạn mức trước buổi báo cáo.
Render/Oracle là phương án tham khảo, không phải máy chủ đang chạy bản demo.
Bucket báo cáo phải riêng tư; MySQL cloud dùng TLS xác minh CA. Database Aiven và local độc lập, không tự đồng bộ.

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
- Local mặc định `MAIL_ENABLED=false`; SMTP dùng `MAIL_PROVIDER=smtp` và `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_AUTH`, `MAIL_STARTTLS`, `MAIL_FROM`.
- Cloud (hoặc localhost) dùng `MAIL_PROVIDER=brevo`, `MAIL_ENABLED=true`, `MAIL_SMTP_HEALTH_ENABLED=false`, `BREVO_API_KEY`, `MAIL_FROM` đã xác minh; khởi động lại sau khi đổi cấu hình. Không commit `.env` hay khóa/mật khẩu thật.
- Email phân công/công bố chỉ được đưa vào hàng đợi sau khi transaction commit; rollback không gửi. Công bố chỉ gửi cho các thành viên của nhóm được duyệt, không gửi lại khi bấm công bố lần hai.
- Queue/SMTP/Brevo lỗi được ghi log, không hủy kết quả đã lưu. Đây chưa phải durable outbox: mất tiến trình hoặc lỗi nhà cung cấp có thể mất email, chưa có retry tự động. Không gọi email đã gửi thành công khi chỉ mới công bố kết quả.
- Excel giữ điểm dạng số, PDF nhúng Noto Sans để giữ dấu tiếng Việt. Font kèm giấy phép SIL OFL tại `src/main/resources/fonts/OFL.txt`, nguồn: https://github.com/notofonts/noto-fonts.
- Sinh viên vào `Kết quả của tôi`, chọn đợt đã đăng ký. Đợt chưa công bố chỉ hiển thị thông báo chờ, không lộ điểm.

## Kiểm thử và bài nộp

```bash
mvn -B clean verify
node --test src/test/js/period-form.test.cjs src/test/js/date-form.test.cjs
```

- `mvn -B verify`: 238 test Java đạt; 35 test JavaScript đạt. JUnit/MockMvc dùng H2; kiểm thử HTTP thực tế ngày 11/10 trên MySQL 8.0.46 đạt 325/325, kiểm tra trình duyệt 41 lượt không lỗi JavaScript, với database QA riêng. Không sửa/xóa dữ liệu demo của người dùng.
- Có hồi quy cho điểm/hội đồng lịch sử, chặn kích hoạt hội đồng rỗng và hoàn tất thiếu phiếu, chống trùng kết quả/export, nộp báo cáo đồng thời và dọn upload sau rollback.
- SMTP/Brevo/Supabase được test bằng máy chủ giả lập. Có minh chứng email phân công thật từ Railway đã tới Inbox; lần kiểm thử cuối không gửi thêm thư thật hoặc tải file thật lên cloud.
- Chưa xác nhận kiểm thử tải lớn, container end-to-end, toàn bộ trình duyệt hoặc toàn bộ email công bố trên cloud.
- Chỉ nộp `Group09_24162035_24162053_24162065.zip`: hai báo cáo `Group9_24162035_24162053_24162065.docx`, `Group9_baocaokq_duan.docx`, mã nguồn, sơ đồ/minh chứng và hướng dẫn tài khoản.
- Sau giải nén, chạy từ `source-code/`; hướng dẫn triển khai nằm duy nhất trong `source-code/deployment/`, không có folder deployment trùng ở thư mục gốc.
- Không kèm `.env`, `.git`, `target`, `uploads`, private key/API key. Không dùng script đóng gói cũ để thay bài nộp đã kiểm tra.

## Trước buổi báo cáo

Kiểm tra `/login`, `/actuator/health`, đăng nhập đúng vai trò, hạn mức Railway và tình trạng Aiven/Supabase/Brevo. Tài khoản demo `admin`, `faculty`, `lecturer1`, `student1` dùng mật khẩu `Password@123`; không dùng lại mật khẩu này cho dịch vụ thật.

Nhập đề tài/nhóm/tài khoản kiểm thử riêng. Để sinh viên xem điểm: khoa khóa phiếu và tính điểm → Chủ tịch xác nhận → khoa công bố. Không chuyển đề tài đã có phiếu sang hội đồng mới hoặc ghi đè phiếu lịch sử.
