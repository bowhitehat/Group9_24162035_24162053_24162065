# Chương 4: Cài đặt & Thử nghiệm Hệ thống (Module Thành viên 2)

## 4.1 Cài đặt Các Thành phần
Module Thành viên 2 đã hoàn thiện đầy đủ các lớp thành phần theo yêu cầu:

1. **Entity & Enum**:
   - `Topic`, `TopicStatus`
   - `StudentGroup`, `GroupMember`
   - `TopicRegistration`, `RegistrationStatus`
   - `ReportSubmission`

2. **Repository Layer**:
   - `TopicRepository`: Hỗ trợ tìm kiếm, lọc theo khoa, đợt, trạng thái và phân trang.
   - `StudentGroupRepository` & `GroupMemberRepository`: Tìm nhóm theo sinh viên, đợt, kiểm tra sinh viên đã có nhóm chưa.
   - `TopicRegistrationRepository`: Kiểm tra trùng đề tài đã đăng ký thành công, lọc danh sách đăng ký.
   - `ReportSubmissionRepository`: Truy vấn lịch sử các phiên bản báo cáo theo thứ tự giảm dần.

3. **Service Layer**:
   - `TopicService`: Nghiệp vụ đề xuất, sửa, gửi duyệt, duyệt, từ chối, công bố đề tài và kiểm tra tối đa 2 GVHD.
   - `StudentGroupService`: Nghiệp vụ tạo nhóm, thêm/xóa thành viên, kiểm tra giới hạn 3 sinh viên, 1 trưởng nhóm, 1 nhóm/đợt.
   - `TopicRegistrationService`: Nghiệp vụ đăng ký đề tài (chỉ trưởng nhóm), duyệt đăng ký, kiểm tra hạn đăng ký, ngăn 2 nhóm đăng ký trùng đề tài.
   - `ReportSubmissionService`: Nghiệp vụ nộp và nộp lại báo cáo, quản lý phiên bản `version`, lưu trữ tệp tin an toàn.

4. **Controller & Thymeleaf UI**:
   - `TopicController` $\rightarrow$ `templates/topics/` (`list.html`, `form.html`, `detail.html`)
   - `StudentGroupController` $\rightarrow$ `templates/groups/` (`my-group.html`)
   - `TopicRegistrationController` $\rightarrow$ `templates/registrations/` (`list.html`)
   - `ReportSubmissionController` $\rightarrow$ `templates/reports/` (`submit.html`, `history.html`)

5. **Phân quyền Backend**:
   - Phân quyền chi tiết trong `SecurityConfig` và `@PreAuthorize` cho các vai trò `LECTURER`, `FACULTY_MANAGER`, `STUDENT`. Xử lý lỗi 403 khi truy cập không hợp lệ.

## 4.2 Thử nghiệm & Kết quả Kiểm thử (Unit Tests)
Đã hoàn thành bộ kiểm thử tự động `Member2BusinessRulesTest` nhằm xác minh tất cả các quy tắc nghiệp vụ bắt buộc:

* **Test Case 1**: Giảng viên tạo đề tài mới thành công ở trạng thái `DRAFT`.
* **Test Case 2**: Báo lỗi khi gán quá 2 Giảng viên hướng dẫn cho một đề tài.
* **Test Case 3**: Khoa duyệt và công bố đề tài thành công.
* **Test Case 4**: Sinh viên tạo nhóm mới và tự động thành Trưởng nhóm.
* **Test Case 5**: Báo lỗi khi thêm sinh viên thứ 4 vào nhóm (vượt quá giới hạn 3 người).
* **Test Case 6**: Báo lỗi khi sinh viên đã thuộc nhóm khác đăng ký tham gia nhóm mới trong cùng một đợt.
* **Test Case 7**: Chỉ Trưởng nhóm được quyền đăng ký đề tài và nộp báo cáo; thành viên thường bị từ chối.
* **Test Case 8**: Ngăn hai nhóm cùng đăng ký thành công một đề tài.
* **Test Case 9**: Tự động tăng số phiên bản `version` khi nộp lại báo cáo.

## 4.3 Kết quả Thực thi Maven Build
* Kết quả chạy `mvn test`: **BUILD SUCCESS**
* Kết quả chạy `mvn clean package`: **BUILD SUCCESS**
