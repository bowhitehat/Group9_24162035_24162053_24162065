# Hợp đồng tích hợp dùng chung

## 1. Công nghệ và phiên bản

- Java 21, Maven Wrapper/Maven 3.9+.
- Spring Boot 3.5.16; Spring MVC, Data JPA, Security, Validation, AOP.
- Thymeleaf, Bootstrap 5.3.8.
- MySQL 8.0 cho `dev`/`prod`; H2 cho `test`.
- Flyway quản lý lược đồ; JUnit 5 kiểm thử.

## 2. Quy ước mã nguồn

- Package gốc: `com.group9.topicmanagement`.
- Phân lớp: `controller` → `service` → `repository` → `domain`.
- Tên class số ít PascalCase; bảng/cột `snake_case`; URL danh từ số nhiều, chữ thường.
- Controller không chứa luật nghiệp vụ; transaction đặt tại service.
- Form Thymeleaf luôn giữ CSRF; quyền được kiểm tra ở `SecurityFilterChain` và `@PreAuthorize`.

## 3. Enum chung

- `RoleName`: `ADMIN`, `FACULTY_MANAGER`, `LECTURER`, `STUDENT`.
- `PeriodType`: `MON_HOC`, `NCKH`, `TLCN`, `KLTN`.
- `PeriodStatus`: `DRAFT` → `LECTURER_REGISTRATION` → `TOPIC_PUBLISHED` → `STUDENT_REGISTRATION` → `IN_PROGRESS` → `GRADING` → `RESULT_PUBLISHED` → `CLOSED`.
- `ReviewerAssignmentStatus`: `ASSIGNED`, `IN_PROGRESS`, `SUBMITTED`, `OVERDUE`, `CANCELLED`.
- `CouncilStatus`: `DRAFT`, `ACTIVE`, `COMPLETED`, `CANCELLED`.
- `CouncilMemberRole`: `CHAIR`, `SECRETARY`, `MEMBER`.
- `EvaluationType`: `REVIEWER`, `COUNCIL_MEMBER`.
- `EvaluationStatus`: `DRAFT`, `SUBMITTED`, `LOCKED`.
- `TopicResultStatus`: `PENDING_CONFIRMATION`, `CONFIRMED`, `PUBLISHED`.
- `AnnouncementStatus`: `DRAFT`, `PUBLISHED`, `ARCHIVED`.

Thành viên 2–3 không tự tạo enum trùng tên; nếu cần giá trị mới phải trao đổi trước khi thêm.

## 4. Entity nền

- `BaseEntity`: `createdAt`, `updatedAt`.
- `User` ↔ `Role`: nhiều-nhiều qua `user_roles`.
- `User` → `Department`: nhiều-một, có thể rỗng.
- `RegistrationPeriod`: khóa ngoại `registration_period_id` dùng chung cho đề tài, nhóm, đăng ký, tiêu chí và hội đồng.

## 5. Quyền

| Vai trò | Phạm vi |
|---|---|
| ADMIN | Tài khoản, vai trò, bộ môn; có thể hỗ trợ quản lý đợt |
| FACULTY_MANAGER | Đợt, duyệt/công bố đề tài, đăng ký, hội đồng, kết quả |
| LECTURER | Đề xuất, hướng dẫn, phản biện, chấm điểm |
| STUDENT | Nhóm, đăng ký đề tài, nộp báo cáo, xem kết quả |

## 6. Migration

- Tên: `V<STT>__<mo_ta>.sql`.
- Đã chia sẻ migration thì không sửa nội dung; thay đổi bằng migration mới.
- **Trang Sĩ Hoàng - 24162035** là người duy nhất phụ trách MySQL Workbench, thiết kế schema, Flyway migration, constraint, index và seed data.
- Thành viên 2 và thành viên 3 chỉ mô tả nhu cầu database trong data contract; không tự tạo/sửa migration, datasource hoặc schema MySQL.
- Schema module thành viên 2 nằm ở V6. Schema module thành viên 3 bắt đầu từ V7 và seed tương ứng ở V8, do thành viên 1 tạo.
- V9: backfill tiêu chí và unique một đăng ký `APPROVED` trên mỗi đề tài.
- V10: bỏ trigger của V9; `RegistrationPeriodService` sinh tiêu chí mặc định trong transaction tạo đợt.
- Khóa ngoại phải có tên; trường tra cứu thường xuyên phải có index; không xóa cứng dữ liệu tham chiếu.

## 7. Liên kết module

- Module thành viên 2 dùng `User`, `Department`, `RegistrationPeriod` để tạo `Topic`, `TopicAdvisor`, `StudentGroup`, `GroupMember`, `TopicRegistration`.
- Module thành viên 2 phụ trách đề tài, nhóm, đăng ký đề tài và nộp báo cáo.
- Module thành viên 3 tham chiếu `TopicRegistration.APPROVED` để tạo phân công phản biện, hội đồng, tiêu chí, đánh giá, kết quả và thông báo.
- Contract chi tiết của module thành viên 3 nằm tại `docs/member3-data-contract.md` trên branch `feature/evaluation-deployment`.
- Không gọi repository của module khác từ controller; đi qua service/contract.
- Upload dùng đường dẫn `app.upload-dir`, không đặt file người dùng trong `static`.
