# Hệ thống quản lý đề tài sinh viên — Group 9

Nhánh `feature/core-admin` là phần của **Trang Sĩ Hoàng — 24162035**, phụ trách nền tảng kỹ thuật, tích hợp, SQL Workbench, tài khoản–phân quyền, bộ môn và đợt đăng ký.

## Thành viên

- Trang Sĩ Hoàng — 24162035.
- Vũ Trọng Hưng — 24162053.
- Trần Hào Kiệt — 24162065.

## Chạy development

1. Cài Java 21, Maven và MySQL 8.
2. Chạy [sql/mysql-workbench-setup.sql](sql/mysql-workbench-setup.sql) trong Workbench sau khi đổi mật khẩu mẫu.
3. Sao chép `.env.example` thành `.env` và điền thông tin cục bộ.
4. Chạy `mvn spring-boot:run` rồi mở `http://localhost:8080/login`.

Khi `DEMO_PASSWORD=Password@123`, hệ thống tạo các tài khoản `admin`, `faculty`, `lecturer1`, `student1`. Đây chỉ là mật khẩu demo cho development.

## Kiểm thử

```bash
mvn test
mvn clean package
```

Quy tắc tích hợp nằm tại [docs/shared-contract.md](docs/shared-contract.md); hướng dẫn Workbench tại [docs/mysql-workbench-guide.md](docs/mysql-workbench-guide.md).
