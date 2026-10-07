# SUBMISSION_CHECKLIST

Tên ZIP bắt buộc: `Group9_24162035_24162053_24162065.zip`

- [x] Mã nguồn Java 21 / Spring Boot / Thymeleaf
- [x] Module thành viên 3: phản biện, hội đồng, chấm điểm, thông báo, dashboard
- [x] Data contract `docs/member3-data-contract.md`
- [x] ERD và mô tả cơ sở dữ liệu trong `diagrams/` và `docs/chapter-4.md`
- [x] Schema tạo từ JPA relationship mapping, dữ liệu mẫu và hướng dẫn MySQL Workbench; không dùng migration SQL/Flyway
- [x] Dockerfile, docker-compose, `deployment/README.md`, health check
- [x] Kiểm thử tích hợp gần nhất ngày 07/10/2026: `mvn -B test` đạt 174/174 lượt test Java; `node --test src/test/js/period-form.test.cjs` đạt 24/24. Test dùng H2; chưa xác nhận lại dữ liệu MySQL/hosting trong lượt này.
- [x] Screenshot Chương 5 từ app chạy thật với MySQL
- [ ] Duyệt bản Word chương 1–6 mới nhất; hiện bản đang làm việc đã đổi tên, chọn bằng `-ReportFile` khi đóng gói
- [ ] URL hosting thật — đang bị chặn vì workspace chưa có tài khoản/credential của nhà cung cấp hosting; không ghi URL giả
- [x] Tạo lại ZIP từ code và Word mới nhất bằng `tools/package_submission.ps1`; script kiểm tra không chứa `.env`, `.git`, `.m2`, `target`, `uploads`, `artifacts`, mật khẩu hoặc token
