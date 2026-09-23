# Hướng dẫn MySQL Workbench — Trang Sĩ Hoàng (24162035)

## Khởi tạo

1. Mở MySQL Workbench và kết nối `Local instance MySQL80`.
2. Mở file `sql/mysql-workbench-setup.sql`.
3. Thay mật khẩu mẫu bằng mật khẩu cục bộ an toàn, chạy toàn bộ script.
4. Sao chép `.env.example` thành `.env`, điền đúng `DB_USERNAME`, `DB_PASSWORD`; `.env` không được commit.
5. Chạy `mvn spring-boot:run`. Flyway tự tạo bảng và dữ liệu tham chiếu.

## Kiểm tra

Trong Workbench, Refresh mục **Schemas**, mở `group9_topic_management` → **Tables**. Chạy:

```sql
SELECT installed_rank, version, description, success
FROM group9_topic_management.flyway_schema_history
ORDER BY installed_rank;
```

Không chỉnh bảng trực tiếp để thay đổi cấu trúc. Mọi thay đổi lược đồ phải là migration Flyway mới.

## V9 và V10

`V9__period_criteria_and_unique_approved_registration.sql`:

- Copy 5 tiêu chí mẫu cho đợt chưa có tiêu chí.
- Cột sinh `approved_topic_id` + `uk_topic_registration_approved_topic`: một đề tài chỉ một đăng ký `APPROVED`.

`V10__move_period_criteria_seed_to_application.sql`:

- Bỏ trigger tạo tiêu chí để hosting MySQL không cần cấp quyền `TRIGGER` cho tài khoản ứng dụng.
- Backfill tiêu chí còn thiếu.
- Các đợt mới nhận 5 tiêu chí trong cùng transaction tại `RegistrationPeriodService`.

Không sửa nội dung V1–V10 sau khi migration đã được chia sẻ hoặc áp dụng. Nếu Flyway báo `checksum mismatch`, phải:

1. Sao lưu database.
2. So sánh schema thực tế với migration chuẩn trong Git.
3. Chỉ chạy `flyway repair` sau khi đã xác nhận schema cuối cùng tương đương.
4. Chạy lại ứng dụng để Flyway áp dụng migration kế tiếp.

Không xóa thủ công dòng trong `flyway_schema_history`; migration có thể chạy lần hai và thất bại vì cột hoặc constraint đã tồn tại. Nếu unique constraint không tạo được do dữ liệu cũ có hai đăng ký `APPROVED` cùng đề tài, phải sửa dữ liệu trùng trước rồi mới migrate.
