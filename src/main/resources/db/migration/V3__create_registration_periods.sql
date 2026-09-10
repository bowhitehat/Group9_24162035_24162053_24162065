CREATE TABLE registration_periods (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  type VARCHAR(20) NOT NULL,
  status VARCHAR(40) NOT NULL,
  lecturer_start DATETIME(6) NOT NULL,
  lecturer_end DATETIME(6) NOT NULL,
  student_start DATETIME(6) NOT NULL,
  student_end DATETIME(6) NOT NULL,
  review_deadline DATETIME(6),
  council_date DATE,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  INDEX idx_period_type_status (type, status),
  INDEX idx_period_student_window (student_start, student_end)
);
