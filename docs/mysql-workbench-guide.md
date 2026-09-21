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

## V9

`V9__period_criteria_and_unique_approved_registration.sql`:

- Copy 5 tiêu chí mẫu cho đợt chưa có tiêu chí.
- Trigger `trg_period_after_insert_criteria`: đợt mới tự có tiêu chí.
- Cột sinh `approved_topic_id` + `uk_topic_registration_approved_topic`: một đề tài chỉ một đăng ký `APPROVED`.

Sau khi kéo code, chạy lại ứng dụng trên MySQL; Flyway ghi version `9` vào `flyway_schema_history`. Nếu unique fail vì dữ liệu cũ có hai `APPROVED` cùng đề tài, sửa dữ liệu rồi chạy lại.
