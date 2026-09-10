INSERT INTO roles (name, created_at, updated_at) VALUES
  ('ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('FACULTY_MANAGER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('LECTURER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('STUDENT', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO departments (code, name, description, active, created_at, updated_at) VALUES
  ('CNPM', 'Công nghệ phần mềm', 'Phụ trách đào tạo và nghiên cứu công nghệ phần mềm.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('HTTT', 'Hệ thống thông tin', 'Phụ trách hệ thống thông tin và dữ liệu.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('MMT', 'Mạng máy tính', 'Phụ trách mạng máy tính và an toàn thông tin.', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
