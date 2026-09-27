# Báo cáo kiểm thử toàn hệ thống

## 1. Phạm vi và môi trường

- Thời điểm kiểm thử: 24/09/2026 (Asia/Ho_Chi_Minh).
- Nhánh kiểm thử: `feature/evaluation-deployment`, đã merge `origin/feature/core-admin` tại commit `7caf75b`.
- Ứng dụng: Java 21, Spring Boot 3.5.16, Thymeleaf, Spring Security, Flyway.
- Cơ sở dữ liệu: MySQL Community Server 8.0.46 thật, schema `group9_topic_management`, Flyway V1–V10 hợp lệ.
- Cách chạy E2E: Maven trực tiếp tại `http://localhost:8090`; MySQL E2E cô lập tại `127.0.0.1:3307`.
- Tài khoản demo dùng trong phiên kiểm thử có mật khẩu chỉ được truyền bằng biến môi trường, không ghi vào mã nguồn, tài liệu hay gói nộp.
- Thư mục upload E2E nằm ngoài repository. Tệp ZIP báo cáo 2.233 byte đã được lưu và đọc lại thành công.

Docker Desktop/CLI không có trên máy kiểm thử nên không thể chạy lại Docker Compose một cách trung thực. Cấu hình tĩnh đã được kiểm tra: `docker-compose.yml` ánh xạ `8080:8080`; theo quy ước của dự án, Docker dùng `http://localhost:8080/login`, còn Maven trực tiếp dùng `http://localhost:8090/login`.

## 2. Kiểm thử tự động

Lệnh thực thi ngày 24/09/2026:

```bash
mvn test
```

Kết quả thật: **44 test, 0 failure, 0 error, 0 skipped — BUILD SUCCESS**.

Các lớp chính: `CoreAdminRequirementsTest`, `Member2BusinessRulesTest`, `Member3BusinessRulesTest`, `CouncilServiceTest`, `ReviewerAssignmentServiceTest`.

## 3. Kết quả E2E 16 bước trên MySQL

| Bước | Thao tác thực tế | Kết quả |
|---:|---|---|
| 1 | Admin tạo `lecturer2`, `lecturer3`, `lecturer4`, `student2`; sau đó tạo `student3` để kiểm tra từ chối truy cập | Đạt |
| 2 | Khoa tạo đợt `Đợt E2E KLTN 2026`; thử dữ liệu thời gian sai trước, hệ thống trả 400; dữ liệu hợp lệ được lưu | Đạt |
| 3 | `lecturer1` tạo đề tài `E2E001 – Hệ thống quản lý đề tài E2E` và gửi duyệt | Đạt |
| 4 | Khoa duyệt đề tài | Đạt |
| 5 | Khoa công bố đề tài; đợt chuyển đúng chuỗi trạng thái đến `GRADING` | Đạt |
| 6 | `student1` tạo nhóm và thêm `student2` | Đạt |
| 7 | Nhóm trưởng đăng ký đề tài `E2E001` | Đạt |
| 8 | Khoa chấp thuận đăng ký | Đạt |
| 9 | Nhóm trưởng upload tệp ZIP báo cáo thật; hệ thống lưu phiên bản 1 và cho tải lại | Đạt |
| 10 | Khoa phân công `lecturer2` phản biện; danh sách hiển thị đúng hạn và trạng thái | Đạt |
| 11 | Khoa lập hội đồng 3 người: `lecturer2` Chủ tịch, `lecturer3` Thư ký, `lecturer4` Thành viên; thử đưa GV hướng dẫn vào hội đồng bị từ chối; hội đồng hợp lệ được kích hoạt | Đạt |
| 12 | Phản biện và ba thành viên hội đồng nhập, lưu/nộp đủ 5 tiêu chí; tổng cộng 4 phiếu và 20 điểm thành phần | Đạt |
| 13 | Khoa khóa cả 4 phiếu; tính điểm tổng kết `8.79` | Đạt |
| 14 | Đăng nhập `lecturer2` với vai trò Chủ tịch và xác nhận; trạng thái thành `CONFIRMED` | Đạt |
| 15 | Khoa công bố; trạng thái kết quả thành `PUBLISHED` | Đạt |
| 16 | `student1` thuộc nhóm xem được điểm `8.79` và toàn bộ điểm thành phần; `student3` ngoài nhóm truy cập cùng URL nhận 403 | Đạt |

Để đi qua toàn bộ cửa sổ nghiệp vụ trong một phiên kiểm thử, sau khi kiểm tra validation thời gian, mốc bắt đầu/kết thúc đăng ký được điều chỉnh trực tiếp trên database E2E. Việc này chỉ mô phỏng thời gian, không bỏ qua kiểm tra quyền hay luật nghiệp vụ.

## 4. Kiểm tra bổ sung

| Hạng mục | Kết quả thật |
|---|---|
| Health check | `GET /actuator/health` trả HTTP 200 và `UP` |
| Database | MySQL 8.0.46; Flyway version 10, success = 1 |
| Dữ liệu kết thúc | 9 users, 1 topic, 1 reviewer assignment, 1 council, 3 council members, 4 evaluations, 20 scores, 1 published result, 1 announcement |
| Đăng nhập | Đăng nhập đúng theo các vai trò Admin/Khoa/Giảng viên/Sinh viên hoạt động; sai mật khẩu bị từ chối |
| Phân quyền | Sinh viên ngoài nhóm nhận 403; giảng viên không được phân công không mở được phiếu; chỉ Chủ tịch xác nhận; chỉ Khoa công bố |
| Static resources | `GET /css/app.css` trả HTTP 200 |
| Upload | Tệp ZIP 2.233 byte lưu ngoài web root, lịch sử hiển thị phiên bản và tải lại được |
| Thông báo/dashboard | Thông báo theo vai trò được công bố và hiển thị trên danh sách cùng dashboard; nội dung tiếng Việt không còn bị mã hóa HTML hai lần |
| Lỗi phát hiện khi E2E | Sửa lazy-loading ở trang tổng hợp/kết quả sinh viên và sửa hiển thị entity HTML của thông báo |

## 5. Ảnh bằng chứng

- `screenshots/08_danh_sach_phan_bien.png`
- `screenshots/09_hoi_dong.png`
- `screenshots/10_form_cham_diem.png`
- `screenshots/11_tong_hop_ket_qua.png`
- `screenshots/12_cong_bo_diem.png`
- `screenshots/13_ket_qua_sinh_vien.png`
- `screenshots/14_thong_bao.png`
- `screenshots/15_dashboard_thong_bao.png`

## 6. Hosting production

Chưa có URL production thật trong phiên kiểm thử này vì workspace không có tài khoản/credential của nhà cung cấp hosting và nhánh chưa được đẩy đến một remote triển khai. Do đó các mục health check, database, đăng nhập, phân quyền, static resource và upload **trên production** chưa thể đánh dấu đạt; không sử dụng URL giả.

Khi triển khai lên nền tảng không có persistent disk, file trong `UPLOAD_DIR` sẽ mất khi container restart/redeploy. Cần gắn persistent volume hoặc chuyển upload sang object storage (S3-compatible) trước khi dùng thực tế.
