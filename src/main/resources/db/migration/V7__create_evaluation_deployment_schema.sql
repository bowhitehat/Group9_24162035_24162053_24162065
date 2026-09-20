CREATE TABLE reviewer_assignments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  topic_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  assigner_id BIGINT NOT NULL,
  assigned_at DATETIME(6) NOT NULL,
  deadline DATETIME(6) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
  submission_time DATETIME(6),
  note VARCHAR(500),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_reviewer_assignment_topic_reviewer UNIQUE (topic_id, reviewer_id),
  CONSTRAINT fk_reviewer_assignment_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
  CONSTRAINT fk_reviewer_assignment_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id),
  CONSTRAINT fk_reviewer_assignment_assigner FOREIGN KEY (assigner_id) REFERENCES users(id),
  CONSTRAINT ck_reviewer_assignment_status CHECK (status IN ('ASSIGNED', 'IN_PROGRESS', 'SUBMITTED', 'OVERDUE', 'CANCELLED')),
  CONSTRAINT ck_reviewer_assignment_deadline CHECK (deadline >= assigned_at),
  INDEX idx_reviewer_assignment_topic_status (topic_id, status),
  INDEX idx_reviewer_assignment_reviewer_status (reviewer_id, status),
  INDEX idx_reviewer_assignment_deadline (deadline)
);

CREATE TABLE councils (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  registration_period_id BIGINT NOT NULL,
  report_date DATETIME(6),
  location VARCHAR(255),
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_council_period FOREIGN KEY (registration_period_id) REFERENCES registration_periods(id),
  CONSTRAINT ck_council_status CHECK (status IN ('DRAFT', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
  INDEX idx_council_period_status (registration_period_id, status),
  INDEX idx_council_report_date (report_date)
);

CREATE TABLE council_members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  council_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  `role` VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_council_member UNIQUE (council_id, member_id),
  CONSTRAINT fk_council_member_council FOREIGN KEY (council_id) REFERENCES councils(id),
  CONSTRAINT fk_council_member_user FOREIGN KEY (member_id) REFERENCES users(id),
  CONSTRAINT ck_council_member_role CHECK (`role` IN ('CHAIR', 'SECRETARY', 'MEMBER')),
  INDEX idx_council_member_role (council_id, `role`),
  INDEX idx_council_member_user (member_id)
);

CREATE TABLE council_assignments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  council_id BIGINT NOT NULL,
  topic_id BIGINT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_council_assignment UNIQUE (council_id, topic_id),
  CONSTRAINT fk_council_assignment_council FOREIGN KEY (council_id) REFERENCES councils(id),
  CONSTRAINT fk_council_assignment_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
  INDEX idx_council_assignment_topic (topic_id)
);

CREATE TABLE evaluation_criteria (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  registration_period_id BIGINT NOT NULL,
  display_order INT NOT NULL,
  is_mandatory BOOLEAN NOT NULL DEFAULT TRUE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_evaluation_criterion_period_name UNIQUE (registration_period_id, name),
  CONSTRAINT uk_evaluation_criterion_period_order UNIQUE (registration_period_id, display_order),
  CONSTRAINT fk_evaluation_criterion_period FOREIGN KEY (registration_period_id) REFERENCES registration_periods(id),
  CONSTRAINT ck_evaluation_criterion_order CHECK (display_order > 0),
  INDEX idx_evaluation_criterion_period_active (registration_period_id, is_active)
);

CREATE TABLE evaluations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  topic_id BIGINT NOT NULL,
  evaluator_id BIGINT NOT NULL,
  evaluation_type VARCHAR(30) NOT NULL,
  reviewer_assignment_id BIGINT,
  council_member_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  comments TEXT,
  submission_time DATETIME(6),
  locked_time DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_evaluation_topic_evaluator_type UNIQUE (topic_id, evaluator_id, evaluation_type),
  CONSTRAINT uk_evaluation_reviewer_assignment UNIQUE (reviewer_assignment_id),
  CONSTRAINT uk_evaluation_topic_council_member UNIQUE (topic_id, council_member_id),
  CONSTRAINT fk_evaluation_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
  CONSTRAINT fk_evaluation_evaluator FOREIGN KEY (evaluator_id) REFERENCES users(id),
  CONSTRAINT fk_evaluation_reviewer_assignment FOREIGN KEY (reviewer_assignment_id) REFERENCES reviewer_assignments(id),
  CONSTRAINT fk_evaluation_council_member FOREIGN KEY (council_member_id) REFERENCES council_members(id),
  CONSTRAINT ck_evaluation_type CHECK (evaluation_type IN ('REVIEWER', 'COUNCIL_MEMBER')),
  CONSTRAINT ck_evaluation_status CHECK (status IN ('DRAFT', 'SUBMITTED', 'LOCKED')),
  CONSTRAINT ck_evaluation_source CHECK (
    (evaluation_type = 'REVIEWER' AND reviewer_assignment_id IS NOT NULL AND council_member_id IS NULL)
    OR
    (evaluation_type = 'COUNCIL_MEMBER' AND reviewer_assignment_id IS NULL AND council_member_id IS NOT NULL)
  ),
  INDEX idx_evaluation_topic_status (topic_id, status),
  INDEX idx_evaluation_evaluator_status (evaluator_id, status)
);

CREATE TABLE evaluation_scores (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  evaluation_id BIGINT NOT NULL,
  criterion_id BIGINT NOT NULL,
  score DECIMAL(4,2) NOT NULL,
  note VARCHAR(500),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_evaluation_score_criterion UNIQUE (evaluation_id, criterion_id),
  CONSTRAINT fk_evaluation_score_evaluation FOREIGN KEY (evaluation_id) REFERENCES evaluations(id),
  CONSTRAINT fk_evaluation_score_criterion FOREIGN KEY (criterion_id) REFERENCES evaluation_criteria(id),
  CONSTRAINT ck_evaluation_score_range CHECK (score >= 0 AND score <= 10),
  INDEX idx_evaluation_score_criterion (criterion_id)
);

CREATE TABLE topic_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  topic_id BIGINT NOT NULL,
  council_assignment_id BIGINT NOT NULL,
  final_score DECIMAL(4,2),
  status VARCHAR(30) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
  confirmer_id BIGINT,
  confirmed_time DATETIME(6),
  publisher_id BIGINT,
  published_time DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_topic_result_topic UNIQUE (topic_id),
  CONSTRAINT uk_topic_result_council_assignment UNIQUE (council_assignment_id),
  CONSTRAINT fk_topic_result_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
  CONSTRAINT fk_topic_result_council_assignment FOREIGN KEY (council_assignment_id) REFERENCES council_assignments(id),
  CONSTRAINT fk_topic_result_confirmer FOREIGN KEY (confirmer_id) REFERENCES users(id),
  CONSTRAINT fk_topic_result_publisher FOREIGN KEY (publisher_id) REFERENCES users(id),
  CONSTRAINT ck_topic_result_score CHECK (final_score IS NULL OR (final_score >= 0 AND final_score <= 10)),
  CONSTRAINT ck_topic_result_status CHECK (status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'PUBLISHED')),
  INDEX idx_topic_result_status (status)
);

CREATE TABLE announcements (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  content LONGTEXT NOT NULL,
  creator_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  published_time DATETIME(6),
  expiration_time DATETIME(6),
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_announcement_creator FOREIGN KEY (creator_id) REFERENCES users(id),
  CONSTRAINT ck_announcement_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
  CONSTRAINT ck_announcement_expiration CHECK (
    expiration_time IS NULL OR published_time IS NULL OR expiration_time > published_time
  ),
  INDEX idx_announcement_status_published (status, published_time),
  INDEX idx_announcement_expiration (expiration_time)
);

CREATE TABLE announcement_target_roles (
  announcement_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  CONSTRAINT pk_announcement_target_roles PRIMARY KEY (announcement_id, role_id),
  CONSTRAINT fk_announcement_target_announcement FOREIGN KEY (announcement_id) REFERENCES announcements(id),
  CONSTRAINT fk_announcement_target_role FOREIGN KEY (role_id) REFERENCES roles(id),
  INDEX idx_announcement_target_role (role_id)
);
