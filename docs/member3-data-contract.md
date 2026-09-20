# Member 3 - Data Contract & Database Specifications

Tài liệu này định nghĩa cấu trúc dữ liệu cho module của Thành viên 3 (Phân công phản biện, Hội đồng, Chấm điểm, Thông báo), dùng làm căn cứ cho Thành viên 1 viết Flyway Migration `V7`.

## 1. Bảng `reviewer_assignments` (Phân công phản biện)
* **Mục đích**: Lưu thông tin phân công giảng viên phản biện cho đề tài.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `topic_id`: `BIGINT NOT NULL` (FK -> `topic.id`)
  - `reviewer_id`: `BIGINT NOT NULL` (FK -> `users.id`)
  - `assigner_id`: `BIGINT` (FK -> `users.id`)
  - `assigned_at`: `DATETIME(6)`
  - `deadline`: `DATETIME(6)`
  - `status`: `VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED'` (Enum: `ASSIGNED`, `IN_PROGRESS`, `SUBMITTED`, `OVERDUE`, `CANCELLED`)
  - `submission_time`: `DATETIME(6)`
  - `note`: `VARCHAR(500)`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc duy nhất & Index**:
  - `UNIQUE (topic_id, reviewer_id)` (Không thêm trùng phân công cho cùng đề tài và giảng viên)
  - `INDEX idx_reviewer_status (status)`

## 2. Bảng `councils` (Hội đồng)
* **Mục đích**: Lưu thông tin hội đồng đánh giá.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `name`: `VARCHAR(255) NOT NULL`
  - `registration_period_id`: `BIGINT NOT NULL` (FK -> `registration_periods.id`)
  - `report_date`: `DATETIME(6)` (Ngày và giờ báo cáo, có thể dùng `DATETIME` hoặc `DATE` + `TIME`, ở đây dùng `DATETIME(6)`)
  - `location`: `VARCHAR(255)`
  - `status`: `VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` (Enum: `DRAFT`, `ACTIVE`, `COMPLETED`, `CANCELLED`)
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Index**:
  - `INDEX idx_council_period (registration_period_id)`
  - `INDEX idx_council_status (status)`

## 3. Bảng `council_members` (Thành viên hội đồng)
* **Mục đích**: Danh sách giảng viên trong hội đồng và vai trò.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `council_id`: `BIGINT NOT NULL` (FK -> `councils.id`)
  - `member_id`: `BIGINT NOT NULL` (FK -> `users.id`)
  - `role`: `VARCHAR(20) NOT NULL` (Enum: `CHAIR`, `SECRETARY`, `MEMBER`)
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc duy nhất**:
  - `UNIQUE (council_id, member_id)` (Không có giảng viên trùng nhau trong cùng hội đồng)

## 4. Bảng `council_assignments` (Đề tài được phân công vào hội đồng)
* **Mục đích**: Liên kết đề tài với hội đồng.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `council_id`: `BIGINT NOT NULL` (FK -> `councils.id`)
  - `topic_id`: `BIGINT NOT NULL` (FK -> `topic.id`)
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc duy nhất**:
  - `UNIQUE (council_id, topic_id)` (Không có đề tài trùng trong cùng hội đồng)
  - Đảm bảo 1 đề tài chỉ được gán vào 1 hội đồng ACTIVE (sẽ kiểm tra ở Service).

## 5. Bảng `evaluation_criteria` (Tiêu chí đánh giá)
* **Mục đích**: Lưu danh sách tiêu chí dùng để chấm điểm.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `name`: `VARCHAR(255) NOT NULL`
  - `description`: `TEXT`
  - `registration_period_id`: `BIGINT NOT NULL` (FK -> `registration_periods.id`)
  - `display_order`: `INT DEFAULT 0`
  - `is_mandatory`: `BOOLEAN DEFAULT TRUE`
  - `is_active`: `BOOLEAN DEFAULT TRUE`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`

## 6. Bảng `evaluations` (Đánh giá của giảng viên)
* **Mục đích**: Lưu phiếu đánh giá tổng thể của một giảng viên cho một đề tài.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `topic_id`: `BIGINT NOT NULL` (FK -> `topic.id`)
  - `evaluator_id`: `BIGINT NOT NULL` (FK -> `users.id`)
  - `evaluation_type`: `VARCHAR(50) NOT NULL` (Loại nhiệm vụ: `REVIEWER`, `COUNCIL_MEMBER`)
  - `status`: `VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` (Enum: `DRAFT`, `SUBMITTED`, `LOCKED`)
  - `comments`: `TEXT`
  - `submission_time`: `DATETIME(6)`
  - `locked_time`: `DATETIME(6)`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc duy nhất & Index**:
  - `UNIQUE (topic_id, evaluator_id, evaluation_type)` (Mỗi giảng viên chỉ có một Evaluation hợp lệ cho một đề tài trong cùng nhiệm vụ chấm)

## 7. Bảng `evaluation_scores` (Điểm từng tiêu chí)
* **Mục đích**: Lưu điểm số cho từng tiêu chí trong phiếu đánh giá.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `evaluation_id`: `BIGINT NOT NULL` (FK -> `evaluations.id`)
  - `criterion_id`: `BIGINT NOT NULL` (FK -> `evaluation_criteria.id`)
  - `score`: `DECIMAL(4,2) NOT NULL`
  - `note`: `VARCHAR(500)`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc duy nhất**:
  - `UNIQUE (evaluation_id, criterion_id)` (Không nhập trùng điểm cho cùng Evaluation và Criterion)

## 8. Bảng `topic_results` (Kết quả đề tài)
* **Mục đích**: Lưu điểm tổng kết cuối cùng của đề tài.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `topic_id`: `BIGINT NOT NULL` (FK -> `topic.id`)
  - `final_score`: `DECIMAL(4,2)`
  - `status`: `VARCHAR(20) NOT NULL DEFAULT 'PENDING_CONFIRMATION'` (Enum: `PENDING_CONFIRMATION`, `CONFIRMED`, `PUBLISHED`)
  - `confirmer_id`: `BIGINT` (FK -> `users.id` - Chủ tịch xác nhận)
  - `confirmed_time`: `DATETIME(6)`
  - `publisher_id`: `BIGINT` (FK -> `users.id` - Faculty Manager công bố)
  - `published_time`: `DATETIME(6)`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc duy nhất**:
  - `UNIQUE (topic_id)`

## 9. Bảng `announcements` (Thông báo)
* **Mục đích**: Quản lý thông báo cho các đối tượng trong hệ thống.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `title`: `VARCHAR(255) NOT NULL`
  - `content`: `LONGTEXT NOT NULL`
  - `creator_id`: `BIGINT NOT NULL` (FK -> `users.id`)
  - `status`: `VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` (Enum: `DRAFT`, `PUBLISHED`, `ARCHIVED`)
  - `target_roles`: `VARCHAR(255)` (Lưu chuỗi các vai trò nhận thông báo, VD: `STUDENT,LECTURER`)
  - `published_time`: `DATETIME(6)`
  - `expiration_time`: `DATETIME(6)`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`

## 10. Seed Data
Yêu cầu Thành viên 1 seed sẵn:
- Một số `evaluation_criteria` cơ bản (Nội dung, Kỹ thuật, Sản phẩm, Báo cáo, Trình bày/phản biện).
- Data mẫu cho `announcements` nếu cần.
- Tài khoản giảng viên và hội đồng để test.
