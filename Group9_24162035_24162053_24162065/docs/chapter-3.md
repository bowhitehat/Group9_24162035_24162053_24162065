# Chương 3: Thiết kế Hệ thống (Module Thành viên 2)

## 3.1 Tổng quan Module Thành viên 2
Module Thành viên 2 phụ trách toàn bộ quy trình đề tài và nhóm sinh viên trong hệ thống Quản lý đề tài khóa luận/luận văn, bao gồm 4 tiểu phân hệ chính:
1. **Quản lý & Duyệt Đề tài**: Giảng viên đề xuất, chỉnh sửa, gửi duyệt; Khoa phê duyệt, từ chối, công bố đề tài.
2. **Quản lý Nhóm Sinh viên**: Sinh viên lập nhóm, chọn trưởng nhóm, thêm/xóa thành viên.
3. **Đăng ký Đề tài**: Trưởng nhóm sinh viên lựa chọn đề tài công bố và đăng ký; Khoa duyệt đăng ký.
4. **Nộp Báo cáo**: Trưởng nhóm nộp báo cáo tiến độ và nộp lại các phiên bản báo cáo mới.

## 3.2 Kiến trúc Phần mềm
Kiến trúc áp dụng nghiêm ngặt theo mô hình 3 lớp chuẩn của Spring Boot:
$$\text{Controller (Web)} \longrightarrow \text{Service (Business Logic)} \longrightarrow \text{Repository (Data Access)} \longrightarrow \text{Database}$$

* **Controller Layer**: Tiếp nhận HTTP Request, validate Form DTO, gọi Service và trả về View Thymeleaf. Không chứa logic nghiệp vụ và không gọi Repository trực tiếp.
* **Service Layer**: Quản lý Transaction (`@Transactional`), thực thi toàn bộ quy tắc nghiệp vụ, kiểm tra ràng buộc dữ liệu.
* **Repository Layer**: Kế thừa `JpaRepository`, cung cấp các truy vấn JPA / JPQL tùy chỉnh có phân trang (`Pageable`).

## 3.3 Thiết kế Thực thể (Entity) & Ràng buộc Nghiệp vụ
1. **Topic (`topic`)**:
   - Trạng thái: `DRAFT` $\rightarrow$ `PENDING` $\rightarrow$ `APPROVED` $\rightarrow$ `PUBLISHED` (hoặc `REJECTED`).
   - Giảng viên hướng dẫn: Tối đa 2 giảng viên (quan hệ nhiều-nhiều qua `topic_advisors`).
2. **StudentGroup (`student_group`) & GroupMember (`group_member`)**:
   - Mỗi nhóm thuộc về 1 đợt đăng ký (`RegistrationPeriod`).
   - Tối đa 3 sinh viên/nhóm. Đúng 1 trưởng nhóm (`is_leader = true`).
   - Một sinh viên không thuộc quá 1 nhóm trong cùng 1 đợt đăng ký (`existsByStudentIdAndPeriodId`).
3. **TopicRegistration (`topic_registration`)**:
   - Chỉ Trưởng nhóm được thực hiện đăng ký đề tài.
   - Kiểm tra hạn đăng ký (`studentRegistrationEndDate`).
   - Đề tài chỉ được phê duyệt đăng ký cho đúng 1 nhóm (`findApprovedRegistrationForTopic`).
4. **ReportSubmission (`report_submission`)**:
   - Đề tài phải ở trạng thái `APPROVED` mới được nộp báo cáo.
   - Hỗ trợ nộp lại bằng cách tự động đánh số phiên bản tăng dần (`version = latestVersion + 1`).

## 3.4 Phân quyền & Bảo mật Backend
Phân quyền truy cập dựa trên vai trò Spring Security:
* `LECTURER`: Đề xuất, sửa, gửi duyệt đề tài của chính mình.
* `FACULTY_MANAGER` / `ADMIN`: Duyệt, từ chối, công bố đề tài; duyệt/từ chối đăng ký đề tài.
* `STUDENT`: Tạo nhóm, quản lý thành viên, đăng ký đề tài, nộp báo cáo.
* Truy cập sai vai trò bị từ chối và phản hồi lại trang lỗi `403 (Forbidden)`.
