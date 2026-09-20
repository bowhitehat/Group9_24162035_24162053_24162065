-- V7__create_evaluation_deployment_schema.sql

-- 1. reviewer_assignments
CREATE TABLE reviewer_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    reviewer_id BIGINT NOT NULL,
    assigner_id BIGINT,
    assigned_at DATETIME(6),
    deadline DATETIME(6),
    status VARCHAR(20) NOT NULL DEFAULT 'ASSIGNED',
    submission_time DATETIME(6),
    note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_reviewer_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
    CONSTRAINT fk_reviewer_user FOREIGN KEY (reviewer_id) REFERENCES users(id),
    CONSTRAINT fk_reviewer_assigner FOREIGN KEY (assigner_id) REFERENCES users(id),
    CONSTRAINT uq_reviewer_topic UNIQUE (topic_id, reviewer_id)
);
CREATE INDEX idx_reviewer_status ON reviewer_assignments(status);

-- 2. councils
CREATE TABLE councils (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    registration_period_id BIGINT NOT NULL,
    report_date DATETIME(6),
    location VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_council_period FOREIGN KEY (registration_period_id) REFERENCES registration_periods(id)
);
CREATE INDEX idx_council_period ON councils(registration_period_id);
CREATE INDEX idx_council_status ON councils(status);

-- 3. council_members
CREATE TABLE council_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    council_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_council_member_council FOREIGN KEY (council_id) REFERENCES councils(id),
    CONSTRAINT fk_council_member_user FOREIGN KEY (member_id) REFERENCES users(id),
    CONSTRAINT uq_council_member UNIQUE (council_id, member_id)
);

-- 4. council_assignments
CREATE TABLE council_assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    council_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_council_assignment_council FOREIGN KEY (council_id) REFERENCES councils(id),
    CONSTRAINT fk_council_assignment_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
    CONSTRAINT uq_council_topic UNIQUE (council_id, topic_id)
);

-- 5. evaluation_criteria
CREATE TABLE evaluation_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    registration_period_id BIGINT NOT NULL,
    display_order INT DEFAULT 0,
    is_mandatory BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_criteria_period FOREIGN KEY (registration_period_id) REFERENCES registration_periods(id)
);

-- 6. evaluations
CREATE TABLE evaluations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    evaluator_id BIGINT NOT NULL,
    evaluation_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    comments TEXT,
    submission_time DATETIME(6),
    locked_time DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_evaluation_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
    CONSTRAINT fk_evaluation_evaluator FOREIGN KEY (evaluator_id) REFERENCES users(id),
    CONSTRAINT uq_evaluation_topic_evaluator UNIQUE (topic_id, evaluator_id, evaluation_type)
);

-- 7. evaluation_scores
CREATE TABLE evaluation_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT NOT NULL,
    criterion_id BIGINT NOT NULL,
    score DECIMAL(4,2) NOT NULL,
    note VARCHAR(500),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_score_evaluation FOREIGN KEY (evaluation_id) REFERENCES evaluations(id),
    CONSTRAINT fk_score_criterion FOREIGN KEY (criterion_id) REFERENCES evaluation_criteria(id),
    CONSTRAINT uq_score_evaluation_criterion UNIQUE (evaluation_id, criterion_id)
);

-- 8. topic_results
CREATE TABLE topic_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    topic_id BIGINT NOT NULL,
    final_score DECIMAL(4,2),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    confirmer_id BIGINT,
    confirmed_time DATETIME(6),
    publisher_id BIGINT,
    published_time DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_result_topic FOREIGN KEY (topic_id) REFERENCES topic(id),
    CONSTRAINT fk_result_confirmer FOREIGN KEY (confirmer_id) REFERENCES users(id),
    CONSTRAINT fk_result_publisher FOREIGN KEY (publisher_id) REFERENCES users(id),
    CONSTRAINT uq_result_topic UNIQUE (topic_id)
);

-- 9. announcements
CREATE TABLE announcements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    creator_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    target_roles VARCHAR(255),
    published_time DATETIME(6),
    expiration_time DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_announcement_creator FOREIGN KEY (creator_id) REFERENCES users(id)
);
