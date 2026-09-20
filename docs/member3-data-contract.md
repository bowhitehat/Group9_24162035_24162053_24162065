# Data contract thành viên 3 - Phản biện, hội đồng, đánh giá và thông báo

Tài liệu này là hợp đồng dữ liệu giữa thành viên 3 và thành viên 1. Thành viên 3 triển khai Java theo đúng contract; **Trang Sĩ Hoàng - 24162035** chịu trách nhiệm tạo và kiểm tra Flyway migration trên MySQL.

## 1. Nguyên tắc chung

- Tái sử dụng `User`, `Role`, `RegistrationPeriod`, `Topic`, `TopicRegistration` và các enum dùng chung; không tạo bản sao.
- Tất cả entity mới kế thừa `BaseEntity`, tương ứng hai cột `created_at`, `updated_at` kiểu `DATETIME(6) NOT NULL`.
- Enum lưu bằng tên viết hoa qua `EnumType.STRING`.
- Mọi foreign key và unique/check constraint phải có tên.
- Cột foreign key, trạng thái, deadline và các trường dùng để lọc phải có index.
- Không xóa cứng dữ liệu đã được tham chiếu. Việc hủy nghiệp vụ thực hiện bằng trạng thái.
- Controller không gọi repository trực tiếp; quyền đối tượng và luật nghiệp vụ được kiểm tra tại service.

## 2. Enum mới

### `ReviewerAssignmentStatus`

`ASSIGNED`, `IN_PROGRESS`, `SUBMITTED`, `OVERDUE`, `CANCELLED`.

### `CouncilStatus`

`DRAFT`, `ACTIVE`, `COMPLETED`, `CANCELLED`.

### `CouncilMemberRole`

`CHAIR`, `SECRETARY`, `MEMBER`.

### `EvaluationType`

`REVIEWER`, `COUNCIL_MEMBER`.

### `EvaluationStatus`

`DRAFT`, `SUBMITTED`, `LOCKED`.

### `TopicResultStatus`

`PENDING_CONFIRMATION`, `CONFIRMED`, `PUBLISHED`.

### `AnnouncementStatus`

`DRAFT`, `PUBLISHED`, `ARCHIVED`.

`CHAIR` là vai trò nghiệp vụ trong `CouncilMember`, không phải Spring Security role.

## 3. Bảng `reviewer_assignments`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `topic_id` | `BIGINT` | `Topic` | NOT NULL |
| `reviewer_id` | `BIGINT` | `User` | NOT NULL |
| `assigner_id` | `BIGINT` | `User` | NOT NULL |
| `assigned_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `deadline` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `status` | `VARCHAR(20)` | `ReviewerAssignmentStatus` | NOT NULL, mặc định `ASSIGNED` |
| `submission_time` | `DATETIME(6)` | `LocalDateTime` | NULL |
| `note` | `VARCHAR(500)` | `String` | NULL |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_reviewer_assignment_topic`: `topic_id -> topic.id`.
- `fk_reviewer_assignment_reviewer`: `reviewer_id -> users.id`.
- `fk_reviewer_assignment_assigner`: `assigner_id -> users.id`.
- `uk_reviewer_assignment_topic_reviewer`: unique `(topic_id, reviewer_id)`.
- `ck_reviewer_assignment_status`: giới hạn đúng các trạng thái đã khai báo.
- `ck_reviewer_assignment_deadline`: `deadline >= assigned_at`.
- `idx_reviewer_assignment_topic_status`: `(topic_id, status)`.
- `idx_reviewer_assignment_reviewer_status`: `(reviewer_id, status)`.
- `idx_reviewer_assignment_deadline`: `(deadline)`.

Một phân công bị hủy và giao lại cho cùng giảng viên phải cập nhật bản ghi cũ, không tạo bản ghi trùng.

## 4. Bảng `councils`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `name` | `VARCHAR(255)` | `String` | NOT NULL |
| `registration_period_id` | `BIGINT` | `RegistrationPeriod` | NOT NULL |
| `report_date` | `DATETIME(6)` | `LocalDateTime` | NULL khi DRAFT; bắt buộc trước ACTIVE |
| `location` | `VARCHAR(255)` | `String` | NULL khi DRAFT; bắt buộc trước ACTIVE |
| `status` | `VARCHAR(20)` | `CouncilStatus` | NOT NULL, mặc định `DRAFT` |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_council_period`: `registration_period_id -> registration_periods.id`.
- `ck_council_status`: giới hạn đúng các trạng thái đã khai báo.
- `idx_council_period_status`: `(registration_period_id, status)`.
- `idx_council_report_date`: `(report_date)`.

## 5. Bảng `council_members`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `council_id` | `BIGINT` | `Council` | NOT NULL |
| `member_id` | `BIGINT` | `User` | NOT NULL |
| `role` | `VARCHAR(20)` | `CouncilMemberRole` | NOT NULL |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_council_member_council`: `council_id -> councils.id`.
- `fk_council_member_user`: `member_id -> users.id`.
- `uk_council_member`: unique `(council_id, member_id)`.
- `ck_council_member_role`: `CHAIR`, `SECRETARY` hoặc `MEMBER`.
- `idx_council_member_role`: `(council_id, role)`.
- `idx_council_member_user`: `(member_id)`.

Số lượng 3-5 người, đúng một CHAIR và đúng một SECRETARY được kiểm tra tại service trước khi kích hoạt hội đồng.

## 6. Bảng `council_assignments`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `council_id` | `BIGINT` | `Council` | NOT NULL |
| `topic_id` | `BIGINT` | `Topic` | NOT NULL |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_council_assignment_council`: `council_id -> councils.id`.
- `fk_council_assignment_topic`: `topic_id -> topic.id`.
- `uk_council_assignment`: unique `(council_id, topic_id)`.
- `idx_council_assignment_topic`: `(topic_id)`.

Một đề tài không được đồng thời thuộc nhiều hội đồng `ACTIVE`; điều kiện phụ thuộc trạng thái hội đồng nên được kiểm tra tại service.

## 7. Bảng `evaluation_criteria`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `name` | `VARCHAR(255)` | `String` | NOT NULL |
| `description` | `TEXT` | `String` | NULL |
| `registration_period_id` | `BIGINT` | `RegistrationPeriod` | NOT NULL |
| `display_order` | `INT` | `Integer` | NOT NULL |
| `is_mandatory` | `BOOLEAN` | `Boolean` | NOT NULL, mặc định TRUE |
| `is_active` | `BOOLEAN` | `Boolean` | NOT NULL, mặc định TRUE |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_evaluation_criterion_period`: `registration_period_id -> registration_periods.id`.
- `uk_evaluation_criterion_period_name`: unique `(registration_period_id, name)`.
- `uk_evaluation_criterion_period_order`: unique `(registration_period_id, display_order)`.
- `ck_evaluation_criterion_order`: `display_order > 0`.
- `idx_evaluation_criterion_period_active`: `(registration_period_id, is_active)`.

## 8. Bảng `evaluations`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `topic_id` | `BIGINT` | `Topic` | NOT NULL |
| `evaluator_id` | `BIGINT` | `User` | NOT NULL |
| `evaluation_type` | `VARCHAR(30)` | `EvaluationType` | NOT NULL |
| `reviewer_assignment_id` | `BIGINT` | `ReviewerAssignment` | NULL, dùng cho REVIEWER |
| `council_member_id` | `BIGINT` | `CouncilMember` | NULL, dùng cho COUNCIL_MEMBER |
| `status` | `VARCHAR(20)` | `EvaluationStatus` | NOT NULL, mặc định `DRAFT` |
| `comments` | `TEXT` | `String` | NULL |
| `submission_time` | `DATETIME(6)` | `LocalDateTime` | NULL |
| `locked_time` | `DATETIME(6)` | `LocalDateTime` | NULL |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_evaluation_topic`: `topic_id -> topic.id`.
- `fk_evaluation_evaluator`: `evaluator_id -> users.id`.
- `fk_evaluation_reviewer_assignment`: `reviewer_assignment_id -> reviewer_assignments.id`.
- `fk_evaluation_council_member`: `council_member_id -> council_members.id`.
- `uk_evaluation_topic_evaluator_type`: unique `(topic_id, evaluator_id, evaluation_type)`.
- `uk_evaluation_reviewer_assignment`: unique `(reviewer_assignment_id)`.
- `uk_evaluation_topic_council_member`: unique `(topic_id, council_member_id)`.
- `ck_evaluation_type`: `REVIEWER` hoặc `COUNCIL_MEMBER`.
- `ck_evaluation_status`: `DRAFT`, `SUBMITTED` hoặc `LOCKED`.
- `ck_evaluation_source`: REVIEWER phải có `reviewer_assignment_id`; COUNCIL_MEMBER phải có `council_member_id`; hai nguồn không được đồng thời có giá trị.
- `idx_evaluation_topic_status`: `(topic_id, status)`.
- `idx_evaluation_evaluator_status`: `(evaluator_id, status)`.

Service phải đối chiếu evaluator với reviewer/member tương ứng và xác nhận đề tài đã được phân công cho đúng nguồn chấm.

## 9. Bảng `evaluation_scores`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `evaluation_id` | `BIGINT` | `Evaluation` | NOT NULL |
| `criterion_id` | `BIGINT` | `EvaluationCriterion` | NOT NULL |
| `score` | `DECIMAL(4,2)` | `BigDecimal` | NOT NULL, từ 0 đến 10 |
| `note` | `VARCHAR(500)` | `String` | NULL |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_evaluation_score_evaluation`: `evaluation_id -> evaluations.id`.
- `fk_evaluation_score_criterion`: `criterion_id -> evaluation_criteria.id`.
- `uk_evaluation_score_criterion`: unique `(evaluation_id, criterion_id)`.
- `ck_evaluation_score_range`: `score >= 0 AND score <= 10`.
- `idx_evaluation_score_criterion`: `(criterion_id)`.

## 10. Bảng `topic_results`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `topic_id` | `BIGINT` | `Topic` | NOT NULL |
| `council_assignment_id` | `BIGINT` | `CouncilAssignment` | NOT NULL |
| `final_score` | `DECIMAL(4,2)` | `BigDecimal` | NULL trước khi tổng hợp; từ 0 đến 10 |
| `status` | `VARCHAR(30)` | `TopicResultStatus` | NOT NULL, mặc định `PENDING_CONFIRMATION` |
| `confirmer_id` | `BIGINT` | `User` | NULL trước khi xác nhận |
| `confirmed_time` | `DATETIME(6)` | `LocalDateTime` | NULL |
| `publisher_id` | `BIGINT` | `User` | NULL trước khi công bố |
| `published_time` | `DATETIME(6)` | `LocalDateTime` | NULL |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_topic_result_topic`: `topic_id -> topic.id`.
- `fk_topic_result_council_assignment`: `council_assignment_id -> council_assignments.id`.
- `fk_topic_result_confirmer`: `confirmer_id -> users.id`.
- `fk_topic_result_publisher`: `publisher_id -> users.id`.
- `uk_topic_result_topic`: unique `(topic_id)`.
- `uk_topic_result_council_assignment`: unique `(council_assignment_id)`.
- `ck_topic_result_score`: `final_score IS NULL OR (final_score >= 0 AND final_score <= 10)`.
- `ck_topic_result_status`: giới hạn đúng các trạng thái đã khai báo.
- `idx_topic_result_status`: `(status)`.

Service phải xác nhận `topic_id` đúng với đề tài của `council_assignment_id`.

## 11. Bảng `announcements`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `id` | `BIGINT` | `Long` | PK, auto increment |
| `title` | `VARCHAR(255)` | `String` | NOT NULL |
| `content` | `LONGTEXT` | `String` | NOT NULL |
| `creator_id` | `BIGINT` | `User` | NOT NULL |
| `status` | `VARCHAR(20)` | `AnnouncementStatus` | NOT NULL, mặc định `DRAFT` |
| `published_time` | `DATETIME(6)` | `LocalDateTime` | NULL |
| `expiration_time` | `DATETIME(6)` | `LocalDateTime` | NULL |
| `created_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |
| `updated_at` | `DATETIME(6)` | `LocalDateTime` | NOT NULL |

Constraint và index:

- `fk_announcement_creator`: `creator_id -> users.id`.
- `ck_announcement_status`: giới hạn đúng các trạng thái đã khai báo.
- `ck_announcement_expiration`: thời gian hết hạn phải sau thời gian công bố khi cả hai có giá trị.
- `idx_announcement_status_published`: `(status, published_time)`.
- `idx_announcement_expiration`: `(expiration_time)`.

Không lưu danh sách vai trò dưới dạng CSV.

## 12. Bảng `announcement_target_roles`

| Cột | MySQL | Java/JPA | Ràng buộc |
|---|---|---|---|
| `announcement_id` | `BIGINT` | `Announcement` | NOT NULL |
| `role_id` | `BIGINT` | `Role` | NOT NULL |

Constraint và index:

- `pk_announcement_target_roles`: primary key `(announcement_id, role_id)`.
- `fk_announcement_target_announcement`: `announcement_id -> announcements.id`.
- `fk_announcement_target_role`: `role_id -> roles.id`.
- `idx_announcement_target_role`: `(role_id)`.

Entity `Announcement` ánh xạ `Set<Role> targetRoles` qua bảng này. Service không cho công bố thông báo khi danh sách đối tượng nhận rỗng.

## 13. Quy tắc chuyển trạng thái

- Reviewer assignment: `ASSIGNED -> IN_PROGRESS -> SUBMITTED`; `ASSIGNED/IN_PROGRESS -> OVERDUE`; `ASSIGNED/IN_PROGRESS -> CANCELLED`.
- Council: `DRAFT -> ACTIVE -> COMPLETED`; `DRAFT/ACTIVE -> CANCELLED`.
- Evaluation: `DRAFT -> SUBMITTED -> LOCKED`.
- Topic result: `PENDING_CONFIRMATION -> CONFIRMED -> PUBLISHED`.
- Announcement: `DRAFT -> PUBLISHED -> ARCHIVED`.

Không cho phép quay lại trạng thái trước. Trường hợp sửa sai sau khóa/công bố phải do quy trình quản trị riêng trong tương lai, không cập nhật trực tiếp.

## 14. Quy tắc nghiệp vụ bắt buộc tại backend

- Chỉ Topic có `TopicRegistration.APPROVED` mới được phân công phản biện hoặc đưa vào hội đồng.
- Reviewer/evaluator không được là proposer hoặc advisor của Topic.
- Hội đồng có 3-5 giảng viên, đúng một CHAIR, đúng một SECRETARY và không có thành viên trùng.
- Ngày hội đồng phải phù hợp với ngày báo cáo của đợt KLTN.
- Chỉ evaluator được phân công mới được tạo/sửa Evaluation tương ứng.
- Evaluation chỉ SUBMITTED khi đủ toàn bộ tiêu chí bắt buộc đang hoạt động của cùng RegistrationPeriod.
- Evaluation LOCKED không được sửa.
- Điểm một giảng viên là trung bình cộng tiêu chí bắt buộc hợp lệ.
- Điểm cuối cùng là trung bình cộng các Evaluation hợp lệ ở trạng thái SUBMITTED hoặc LOCKED, làm tròn hai chữ số bằng `RoundingMode.HALF_UP`.
- Không tạo TopicResult hoặc công bố khi thiếu điểm bắt buộc.
- CHAIR của đúng hội đồng xác nhận TopicResult; chỉ FACULTY_MANAGER công bố.
- TopicResult PUBLISHED không được sửa.
- STUDENT chỉ xem TopicResult PUBLISHED của Topic thuộc nhóm mình.
- Nội dung Announcement được render an toàn; không dùng `th:utext` cho dữ liệu chưa làm sạch.

## 15. Seed data

Migration seed do thành viên 1 tạo cho các RegistrationPeriod đã tồn tại:

| Thứ tự | Tên tiêu chí | Bắt buộc | Hoạt động |
|---:|---|---|---|
| 1 | Nội dung | Có | Có |
| 2 | Kỹ thuật | Có | Có |
| 3 | Sản phẩm | Có | Có |
| 4 | Báo cáo | Có | Có |
| 5 | Trình bày/phản biện | Có | Có |

Không seed thông báo giả. Tài khoản và role mẫu tiếp tục dùng dữ liệu của migration nền. Với RegistrationPeriod tạo mới sau này, service của thành viên 3 phải tạo bộ tiêu chí mặc định hoặc FACULTY_MANAGER tạo thủ công trước giai đoạn chấm điểm.

## 16. Trách nhiệm migration

- Thành viên 3 không tạo hoặc sửa file migration và không thay đổi datasource.
- Thành viên 1 tạo schema V7 và seed V8 từ contract này, kiểm tra trên MySQL mới và MySQL đang ở V6.
- Nếu entity cần thêm cột hoặc constraint, thành viên 3 cập nhật contract và trao đổi trước; thay đổi database được thực hiện bằng migration mới do thành viên 1 tạo.
