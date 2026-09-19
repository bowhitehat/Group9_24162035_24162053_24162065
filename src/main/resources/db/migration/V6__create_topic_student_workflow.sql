ALTER TABLE registration_periods
  ADD COLUMN report_submission_deadline DATETIME(6) NULL AFTER student_end;

CREATE TABLE topic (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description LONGTEXT,
  requirement LONGTEXT,
  department_id BIGINT NOT NULL,
  registration_period_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  proposer_id BIGINT NOT NULL,
  rejection_reason VARCHAR(500),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_topic_code_period UNIQUE (code, registration_period_id),
  CONSTRAINT fk_topic_department FOREIGN KEY (department_id) REFERENCES departments(id),
  CONSTRAINT fk_topic_period FOREIGN KEY (registration_period_id) REFERENCES registration_periods(id),
  CONSTRAINT fk_topic_proposer FOREIGN KEY (proposer_id) REFERENCES users(id),
  INDEX idx_topic_period_department (registration_period_id, department_id),
  INDEX idx_topic_status (status)
);

CREATE TABLE topic_advisors (
  topic_id BIGINT NOT NULL,
  advisor_id BIGINT NOT NULL,
  CONSTRAINT pk_topic_advisors PRIMARY KEY (topic_id, advisor_id),
  CONSTRAINT fk_topic_advisor_topic FOREIGN KEY (topic_id) REFERENCES topic(id) ON DELETE CASCADE,
  CONSTRAINT fk_topic_advisor_user FOREIGN KEY (advisor_id) REFERENCES users(id)
);

CREATE TABLE student_group (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  registration_period_id BIGINT NOT NULL,
  leader_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_group_period_leader UNIQUE (registration_period_id, leader_id),
  CONSTRAINT fk_group_period FOREIGN KEY (registration_period_id) REFERENCES registration_periods(id),
  CONSTRAINT fk_group_leader FOREIGN KEY (leader_id) REFERENCES users(id),
  INDEX idx_group_period (registration_period_id)
);

CREATE TABLE group_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  is_leader BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_group_member UNIQUE (group_id, member_id),
  CONSTRAINT fk_group_member_group FOREIGN KEY (group_id) REFERENCES student_group(id) ON DELETE CASCADE,
  CONSTRAINT fk_group_member_user FOREIGN KEY (member_id) REFERENCES users(id),
  INDEX idx_group_member_user (member_id)
);

CREATE TABLE topic_registration (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_group_id BIGINT NOT NULL,
  topic_id BIGINT NOT NULL,
  registration_period_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  approver_id BIGINT,
  rejection_reason VARCHAR(500),
  approved_at DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_registration_group_period UNIQUE (student_group_id, registration_period_id),
  CONSTRAINT fk_registration_group FOREIGN KEY (student_group_id) REFERENCES student_group(id),
  CONSTRAINT fk_registration_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
  CONSTRAINT fk_registration_period FOREIGN KEY (registration_period_id) REFERENCES registration_periods(id),
  CONSTRAINT fk_registration_approver FOREIGN KEY (approver_id) REFERENCES users(id),
  INDEX idx_reg_status (status),
  INDEX idx_reg_topic_period_status (topic_id, registration_period_id, status)
);

CREATE TABLE report_submission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_group_id BIGINT NOT NULL,
  topic_registration_id BIGINT NOT NULL,
  submitter_id BIGINT NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  stored_file_name VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  version INT NOT NULL DEFAULT 1,
  note VARCHAR(500),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_report_registration_version UNIQUE (topic_registration_id, version),
  CONSTRAINT fk_report_group FOREIGN KEY (student_group_id) REFERENCES student_group(id),
  CONSTRAINT fk_report_registration FOREIGN KEY (topic_registration_id) REFERENCES topic_registration(id),
  CONSTRAINT fk_report_submitter FOREIGN KEY (submitter_id) REFERENCES users(id),
  INDEX idx_report_group_topic (student_group_id, topic_registration_id)
);
