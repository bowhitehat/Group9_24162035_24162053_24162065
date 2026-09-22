# Báo cáo kiểm thử toàn hệ thống

Thành viên 3 phụ trách kiểm thử module đánh giá và chuẩn bị kịch bản E2E sau khi thành viên 1 tích hợp đủ ba module.

## Kiểm thử module (JUnit)

Chạy:

```bash
mvn test
mvn clean package
```

Các case module thành viên 3 nằm tại `Member3BusinessRulesTest`, `CouncilServiceTest`, `ReviewerAssignmentServiceTest`.

## Kịch bản E2E (sau tích hợp trên MySQL)

1. Admin tạo tài khoản.
2. Khoa tạo đợt đăng ký.
3. Giảng viên đề xuất đề tài.
4. Khoa duyệt đề tài.
5. Khoa công bố đề tài.
6. Sinh viên tạo nhóm.
7. Nhóm trưởng đăng ký đề tài.
8. Khoa chấp thuận đăng ký.
9. Nhóm trưởng nộp báo cáo.
10. Khoa phân công phản biện.
11. Khoa thành lập hội đồng.
12. Giảng viên nhập và nộp điểm.
13. Khoa khóa điểm.
14. Chủ tịch xác nhận kết quả.
15. Khoa công bố kết quả.
16. Sinh viên xem kết quả nhóm mình.

Bổ sung: đăng nhập đúng/sai, điều hướng theo vai trò, 403 khi trái phép, CSRF form, Flyway, static, upload, trang lỗi, giao diện responsive cơ bản.

Kết quả E2E trên hosting thật sẽ được ghi sau khi có URL và dữ liệu demo.
