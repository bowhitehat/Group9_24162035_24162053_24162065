# Member 2 - Integration Guide

Hướng dẫn tích hợp cho Thành viên 1 (Core & Admin) và Thành viên 3 (Hội đồng & Chấm điểm) với Module Đề tài và Nhóm sinh viên của Thành viên 2.

---

## 1. Tích hợp với Thành viên 1 (Core & Admin)

### 1.1 Quản lý Lược đồ DB (Flyway Migration)
* Thành viên 2 đã khai báo đầy đủ các Entity JPA và cấu hình Hibernate tự động đồng bộ lược đồ trên H2 trong môi trường Test.
* Migration MySQL đã được tích hợp tại `V6__create_topic_student_workflow.sql`; không bật Hibernate tự tạo bảng ở môi trường dev/prod.

### 1.2 Sử dụng các Entity dùng chung
* Thành viên 2 đã tích hợp trực tiếp các Entity từ Thành viên 1: `User`, `Department`, `RegistrationPeriod`.
* Quá trình xác thực người dùng dựa vào `UserRepository` và thông tin `Principal` của Spring Security (`username`).

---

## 2. Tích hợp với Thành viên 3 (Hội đồng & Chấm điểm)

### 2.1 Lấy danh sách đăng ký đề tài chính thức (`TopicRegistration`)
* Thành viên 3 chỉ phân công hội đồng và chấm điểm cho các đăng ký đề tài đã được phê duyệt chính thức (`status = RegistrationStatus.APPROVED`).
* API/Service cung cấp bởi Thành viên 2:
  ```java
  TopicRegistration registration = topicRegistrationService.getRegistrationById(registrationId);
  ```

### 2.2 Lấy báo cáo mới nhất của sinh viên (`ReportSubmission`)
* Hội đồng chấm điểm có thể lấy bản báo cáo mới nhất (phiên bản `version` cao nhất) của nhóm sinh viên bằng cách gọi `ReportSubmissionService`:
  ```java
  List<ReportSubmission> history = reportSubmissionService.getSubmissionHistoryForEvaluation(registrationId);
  // Phần tử đầu tiên trong danh sách history là bản nộp mới nhất (phiên bản cao nhất)
  ReportSubmission latestReport = history.isEmpty() ? null : history.get(0);
  ```

### 2.3 Ràng buộc phân quyền
* Thành viên 3 khi phát triển chức năng cho Giảng viên chấm điểm (`LECTURER`) hoặc Trưởng khoa/Chủ tịch hội đồng (`FACULTY_MANAGER`) cần tuân thủ cấu hình bảo mật trong `SecurityConfig.java` hoặc sử dụng `@PreAuthorize`.
