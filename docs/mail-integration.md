# Tích hợp email thông báo

TV1 cung cấp EmailService và cấu hình SMTP. TV3 nối service vào phân công phản biện và công bố kết quả. Chức năng nghiệp vụ không tự động gửi mail chỉ vì đã cấu hình SMTP.

## Cấu hình

Mặc định MAIL_ENABLED=false: ứng dụng không liên hệ SMTP và trả DISABLED, không báo đã gửi. Sao chép các biến MAIL_* từ .env.example vào .env riêng. Không commit .env, mật khẩu hay app password.

Sandbox SMTP trên máy: MAIL_ENABLED=true, MAIL_HOST=localhost, MAIL_PORT=1025, MAIL_AUTH=false, MAIL_STARTTLS=false. Sandbox phải chạy thật trước khi thử.

SMTP production: dùng host/port của nhà cung cấp, MAIL_AUTH=true và MAIL_STARTTLS=true khi nhà cung cấp yêu cầu STARTTLS; MAIL_FROM phải là địa chỉ được phép gửi. Không gửi dữ liệu sinh viên thật khi thử.

Docker: localhost trong container là chính container, không phải máy Windows. MAIL_HOST phải là tên service SMTP trong Compose hoặc host.docker.internal khi SMTP chạy trên máy Windows.

## Contract cho thành viên 3

Gọi bean EmailService.sendNotification(recipient, subject, body), mỗi sinh viên một email riêng. Hàm trả CompletableFuture<DeliveryStatus>: SENT (SMTP đã nhận, chưa chứng minh người dùng nhận được), DISABLED, FAILED hoặc INVALID.

Chỉ gọi sau transaction công bố/phân công đã commit, ví dụ listener sự kiện AFTER_COMMIT. Không gọi trước commit hoặc trong vòng lặp trước khi xác nhận kết quả. Giữ dữ liệu cần gửi dưới dạng chuỗi/ID, không chuyển entity lazy sang thread mail.

TV3 cần chống công bố/gửi trùng; không gửi điểm chưa PUBLISHED; không gộp địa chỉ sinh viên vào To/CC. MAIL_ENABLED=false hoặc FAILED không được hiển thị thông báo “đã gửi email”.

Executor có 1–2 worker, hàng đợi 100 tác vụ, timeout SMTP 5 giây. Nếu hàng đợi đầy, caller phải xử lý TaskRejectedException tại bước thông báo sau commit, không đổi kết quả nghiệp vụ thành thất bại. Không có cơ chế retry bền vững trong module này.

## Kiểm thử

EmailServiceTest kiểm tra tắt mail, nội dung/người nhận, lỗi SMTP và dữ liệu không hợp lệ bằng mock. Kiểm thử SMTP cục bộ riêng xác minh kết nối và nội dung; kiểm thử này không chứng minh thư đến hộp thư Internet.

TV3 bổ sung test sự kiện công bố, rollback không gửi, gửi cho tất cả thành viên, không gửi trùng và xử lý hàng đợi/lỗi mail.
