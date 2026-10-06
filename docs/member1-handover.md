# Bàn giao phần TV1 — Trang Sĩ Hoàng

## Đã triển khai và kiểm tra

- Tạo/sửa tài khoản và bộ môn, thông báo lỗi/thành công, chống trùng username/email/MSSV/mã bộ môn. Sửa hồ sơ không thay đổi ID hoặc mật khẩu. Không tự bỏ ADMIN/đổi username đang đăng nhập.
- Chuẩn hóa tên FK trong relationship mapping toàn hệ thống; unique MSSV. Không bổ sung SQL/JPQL viết tay. Database MySQL đang dùng không bị xóa/tạo lại trong phiên làm này.
- SMTP qua biến môi trường, EmailService bất đồng bộ, UTF-8, timeout, trạng thái gửi rõ ràng; mặc định tắt. Contract trong mail-integration.md.
- Layout mobile có menu và link đăng ký/nộp báo cáo của sinh viên; form admin có label và phản hồi thao tác.
- Use-case tổng thể, FR/NFR, ma trận quyền, phân công đến lớp, từ điển dữ liệu và nội dung nguồn Chương 1–2 cập nhật.
- Build đạt 144 test Java; 24 test JavaScript; kiểm tra web thật bằng H2 độc lập; SMTP cục bộ bằng JavaMailSender thật.

## Cần chốt/phối hợp trước nộp cuối

1. Trưởng khoa và trưởng bộ môn đang dùng chung FACULTY_MANAGER. Nếu rubric yêu cầu phạm vi theo bộ môn, nhóm cần chọn convention rồi mới thay đổi bảo mật/contract đồng bộ.
2. TV2 hoàn thiện nhóm 3–5, xóa nháp hợp lệ, tìm kiếm/phân trang. approveRegistration hiện kiểm tra đăng ký đã APPROVED nhưng chưa khóa Topic khi duyệt đồng thời; không còn unique cột sinh từ SQL cũ.
3. TV3 nối email sau commit, mỗi thành viên một thư, chống gửi trùng và xử lý queue đầy/SMTP lỗi; bổ sung xuất Excel/PDF và các phần đã giao. EmailService hiện không tự gửi khi công bố.
4. Chạy lại E2E MySQL và build sau tích hợp TV2/TV3; kiểm tra dữ liệu MSSV hiện có không trùng trước khi Hibernate thêm unique. Không coi test H2 là bằng chứng MySQL mới đã đạt.
5. Ghép nội dung Chương 1–2 vào Word cuối sau khi các module chốt. Phiên này cập nhật nguồn Markdown, không thay thế file Word đang mở/chưa kiểm tra bố cục.

Code TV1 và contract được bàn giao trên nhánh feature/core-admin. Các thay đổi Word/ERD và tài liệu riêng TV2/TV3 trong workspace tiếp tục được giữ để hoàn thiện bài nộp.
