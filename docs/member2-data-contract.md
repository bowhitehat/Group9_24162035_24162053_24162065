# Member 2 - Data Contract & Database Specifications

Tài liệu này chi tiết hóa các bảng, cột, khóa ngoại, ràng buộc duy nhất và chỉ mục (index) cho module của Thành viên 2 (Topic & Student Workflow), dùng làm căn cứ cho Thành viên 1 viết các Flyway Migration kế tiếp.

---

## 1. Bảng `topic` (Đề tài)
* **Mục đích**: Lưu thông tin đề tài do giảng viên đề xuất.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `code`: `VARCHAR(20) NOT NULL`
  - `title`: `VARCHAR(255) NOT NULL`
  - `description`: `LONGTEXT` (hoặc `TEXT`)
  - `requirement`: `LONGTEXT` (hoặc `TEXT`)
  - `department_id`: `BIGINT NOT NULL` (FK -> `department.id`)
  - `registration_period_id`: `BIGINT NOT NULL` (FK -> `registration_period.id`)
  - `status`: `VARCHAR(20) NOT NULL DEFAULT 'DRAFT'`
  - `proposer_id`: `BIGINT NOT NULL` (FK -> `users.id`)
  - `rejection_reason`: `VARCHAR(500)`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc duy nhất & Index**:
  - `UNIQUE (code, registration_period_id)`
  - `INDEX idx_topic_period_department (registration_period_id, department_id)`
  - `INDEX idx_topic_status (status)`

---

## 2. Bảng `topic_advisors` (Giảng viên hướng dẫn)
* **Mục đích**: Liên kết nhiều-nhiều giữa đề tài và giảng viên hướng dẫn (tối đa 2).
* **Cột**:
  - `topic_id`: `BIGINT NOT NULL` (FK -> `topic.id`)
  - `advisor_id`: `BIGINT NOT NULL` (FK -> `users.id`)
* **Ràng buộc**:
  - `PRIMARY KEY (topic_id, advisor_id)`

---

## 3. Bảng `student_group` (Nhóm sinh viên)
* **Mục đích**: Lưu thông tin nhóm sinh viên tham gia trong một đợt.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `registration_period_id`: `BIGINT NOT NULL` (FK -> `registration_period.id`)
  - `leader_id`: `BIGINT NOT NULL` (FK -> `users.id`)
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc**:
  - `UNIQUE (registration_period_id, leader_id)`

---

## 4. Bảng `group_member` (Thành viên nhóm)
* **Mục đích**: Lưu danh sách thành viên thuộc nhóm sinh viên.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `group_id`: `BIGINT NOT NULL` (FK -> `student_group.id`)
  - `member_id`: `BIGINT NOT NULL` (FK -> `users.id`)
  - `is_leader`: `BOOLEAN NOT NULL DEFAULT FALSE`
* **Ràng buộc**:
  - `UNIQUE (group_id, member_id)`

---

## 5. Bảng `topic_registration` (Đăng ký đề tài)
* **Mục đích**: Quản lý việc nhóm sinh viên đăng ký đề tài trong một đợt.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `student_group_id`: `BIGINT NOT NULL` (FK -> `student_group.id`)
  - `topic_id`: `BIGINT NOT NULL` (FK -> `topic.id`)
  - `registration_period_id`: `BIGINT NOT NULL` (FK -> `registration_period.id`)
  - `status`: `VARCHAR(20) NOT NULL DEFAULT 'PENDING'`
  - `approver_id`: `BIGINT` (FK -> `users.id`)
  - `rejection_reason`: `VARCHAR(500)`
  - `approved_at`: `DATETIME(6)`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Ràng buộc duy nhất & Index**:
  - `UNIQUE (student_group_id, registration_period_id)`
  - `INDEX idx_reg_status (status)`

---

## 6. Bảng `report_submission` (Nộp báo cáo)
* **Mục đích**: Quản lý việc nộp và nộp lại báo cáo tiến độ/khóa luận của nhóm sinh viên.
* **Cột**:
  - `id`: `BIGINT AUTO_INCREMENT PRIMARY KEY`
  - `student_group_id`: `BIGINT NOT NULL` (FK -> `student_group.id`)
  - `topic_registration_id`: `BIGINT NOT NULL` (FK -> `topic_registration.id`)
  - `submitter_id`: `BIGINT NOT NULL` (FK -> `users.id`)
  - `original_file_name`: `VARCHAR(255) NOT NULL`
  - `stored_file_name`: `VARCHAR(255) NOT NULL`
  - `content_type`: `VARCHAR(100) NOT NULL`
  - `file_size`: `BIGINT NOT NULL`
  - `version`: `INT NOT NULL DEFAULT 1`
  - `note`: `VARCHAR(500)`
  - `created_at`: `DATETIME(6) NOT NULL`
  - `updated_at`: `DATETIME(6) NOT NULL`
* **Index**:
  - `INDEX idx_report_group_topic (student_group_id, topic_registration_id)`
