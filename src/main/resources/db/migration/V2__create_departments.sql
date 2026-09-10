CREATE TABLE departments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(20) NOT NULL,
  name VARCHAR(160) NOT NULL,
  description VARCHAR(1000),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_department_code UNIQUE (code)
);

ALTER TABLE users ADD COLUMN department_id BIGINT NULL;
ALTER TABLE users ADD CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES departments(id);
CREATE INDEX idx_user_department ON users(department_id);
