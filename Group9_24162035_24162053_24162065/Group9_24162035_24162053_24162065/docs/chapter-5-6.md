# Chương 5: Triển khai và Tích hợp hệ thống
## 5.1. Môi trường triển khai
- Hệ điều hành: Windows/Linux
- Cơ sở dữ liệu: MySQL 8.0 (Dockerized)
- Nền tảng: Java 21, Spring Boot 3
- Containerization: Docker & Docker Compose

## 5.2. Quá trình tích hợp (CI/CD)
Toàn bộ source code được quản lý qua Git. Quá trình tích hợp 3 phân hệ (Core Admin, Đăng ký Đề tài, Phân công & Chấm điểm) đã hoàn tất và kiểm tra qua Unit Tests.

# Chương 6: Kết luận và Hướng phát triển
## 6.1. Kết quả đạt được
- Hệ thống hỗ trợ đầy đủ luồng từ khởi tạo tài khoản, đăng ký đề tài đến bảo vệ và chấm điểm.
- Giao diện thân thiện, bảo mật phân quyền nghiêm ngặt.

## 6.2. Hướng phát triển
- Tích hợp gửi email tự động khi có thông báo mới.
- Hỗ trợ xuất file PDF điểm tổng kết trực tiếp.
