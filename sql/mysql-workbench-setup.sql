-- Chạy bằng tài khoản quản trị trong MySQL Workbench.
-- Đổi CHANGE_ME_STRONG_PASSWORD trước khi chạy trên máy khác.
CREATE DATABASE IF NOT EXISTS group9_topic_management
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'group9_app'@'localhost'
  IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
ALTER USER 'group9_app'@'localhost'
  IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON group9_topic_management.* TO 'group9_app'@'localhost';
FLUSH PRIVILEGES;
