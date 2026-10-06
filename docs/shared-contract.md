# Hợp đồng tích hợp dùng chung

## 1. Công nghệ và phiên bản

- Java 21, Maven Wrapper/Maven 3.9+.
- Spring Boot 3.5.16; Spring MVC, Data JPA, Security, Validation, AOP.
- Thymeleaf, Bootstrap 5.3.8.
- MySQL 8.0 cho `dev`/`prod`; H2 cho `test`.
- Hibernate ORM quản lý lược đồ từ entity mapping; JUnit 5 kiểm thử.

## 2. Quy ước mã nguồn

- Package gốc: `com.group9.topicmanagement`.
- Phân lớp: `controller` → `service` → `repository` → `model`.
- Các lớp nhận dữ liệu form/DTO nằm trong package `com.group9.topicmanagement.dto` (ví dụ `UserForm`, `PeriodForm`, `EvaluationSubmitDto`); không đặt trong `controller.form`.
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

## 6. JPA relationship mapping

- Không viết file SQL hoặc `@Query`; schema được tạo từ entity và relationship mapping.
- **Trang Sĩ Hoàng - 24162035** phụ trách MySQL Workbench, entity mapping, constraint, index và dữ liệu nền bằng JPA.
- Thành viên 2 và thành viên 3 mô tả nhu cầu dữ liệu trong contract; mọi thay đổi phải thể hiện bằng annotation JPA và repository method.
- `RegistrationPeriodService` sinh tiêu chí mặc định trong transaction tạo đợt.
- Khóa ngoại phải có tên; trường tra cứu thường xuyên phải có index; không xóa cứng dữ liệu tham chiếu.

## 7. Liên kết module

- Module thành viên 2 dùng `User`, `Department`, `RegistrationPeriod` để tạo `Topic`, `TopicAdvisor`, `StudentGroup`, `GroupMember`, `TopicRegistration`.
- Module thành viên 2 phụ trách đề tài, nhóm, đăng ký đề tài và nộp báo cáo.
- Module thành viên 3 tham chiếu `TopicRegistration.APPROVED` để tạo phân công phản biện, hội đồng, tiêu chí, đánh giá, kết quả và thông báo.
- Contract chi tiết của module thành viên 3 nằm tại `docs/member3-data-contract.md` trên branch `feature/evaluation-deployment`.
- Không gọi repository của module khác từ controller; đi qua service/contract.
- Upload dùng đường dẫn `app.upload-dir`, không đặt file người dùng trong `static`.

## 8. Hạ tầng TV1 bổ sung

- Admin có GET/POST /admin/users/{id}/edit và /admin/departments/{id}/edit; không sửa password hash qua form hồ sơ.
- Username/email/MSSV được chuẩn hóa và kiểm tra trùng khi tạo/sửa. student_code có unique constraint qua JPA; MSSV bỏ trống lưu NULL.
- FK trong model có tên annotation; giữ nguyên package, bảng, role và quan hệ, không tạo lại entity hoặc SQL.
- Trưởng khoa/trưởng bộ môn hiện dùng chung FACULTY_MANAGER; chưa triển khai giới hạn theo bộ môn. Chỉ tách khi đã chốt yêu cầu.
- EmailService.sendNotification trả CompletableFuture<DeliveryStatus>. Contract trong docs/mail-integration.md; TV3 chỉ gọi sau commit.
- Yêu cầu/ma trận quyền/phân công chi tiết trong docs/system-requirements.md. FR05 3–5 SV, email nghiệp vụ và export là phần TV2/TV3 còn phải tích hợp.
