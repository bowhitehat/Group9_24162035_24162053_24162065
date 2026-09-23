# Hướng dẫn Ghép Báo cáo & Tài liệu Module Thành viên 2 (Gửi Thành viên 3)

Tài liệu này tổng hợp toàn bộ thông tin, dữ liệu, sơ đồ và hình ảnh thuộc **Module Thành viên 2 (Quản lý Đề tài, Nhóm sinh viên, Đăng ký Đề tài & Nộp Báo cáo)** để **Thành viên 3** làm đầu mối ghép vào Báo cáo Đồ án tổng thể của Nhóm 9.

---

## 1. Thư mục Tài liệu & File đính kèm

Tất cả các tài liệu và hình ảnh đã được chuẩn bị đầy đủ tại thư mục `docs/`:

1. **Nội dung Chương 3 (Hoàn chỉnh)**: [chapter-3.md](chapter-3.md)
   - Thiết kế sơ đồ Use case
   - Danh sách Tác nhân (Actors) & phân quyền RBAC
   - Yêu cầu chức năng (Functional Requirements: FR-01 đến FR-12)
   - Yêu cầu phi chức năng (Non-Functional Requirements: NFR-01 đến NFR-05)
2. **File Sơ đồ Use Case**:
   - File thiết kế gốc DrawIO: [use-case-member2.drawio](use-case-member2.drawio)
   - File ảnh PNG sơ đồ: [use-case-member2.png](use-case-member2.png)
3. **Bộ Ảnh chụp Giao diện Module Thành viên 2**:
   - Giao diện Danh sách Đề tài: [member2_topics_list.png](member2_topics_list.png)
   - Giao diện Đề xuất Đề tài: [member2_topic_detail.png](member2_topic_detail.png)
   - Giao diện Nhóm Sinh viên: [member2_student_group.png](member2_student_group.png)
   - Giao diện Nộp Báo cáo & Phiên bản: [member2_report_submission.png](member2_report_submission.png)

---

## 2. Tóm tắt các Quy tắc Nghiệp vụ Module 2 (Để Ghép Báo cáo & Kiểm thử)

1. **Quản lý Đề tài**:
   - Giảng viên đề xuất đề tài (`DRAFT`), sửa đề tài, gửi duyệt (`PENDING`).
   - Mỗi đề tài có tối đa **2 Giảng viên hướng dẫn**.
   - Khoa duyệt (`APPROVED`), từ chối (`REJECTED` kèm lý do), và Công bố (`PUBLISHED`).
2. **Nhóm Sinh viên**:
   - Sinh viên tạo nhóm trong đợt đăng ký (người tạo là Trưởng nhóm `is_leader = true`).
   - Mỗi nhóm **tối đa 3 sinh viên**. Mỗi sinh viên chỉ thuộc **tối đa 1 nhóm** trong cùng 1 đợt.
3. **Đăng ký Đề tài**:
   - Chỉ **Trưởng nhóm** mới được đăng ký đề tài `PUBLISHED`.
   - Đề tài chỉ được phê duyệt đăng ký cho **đúng 1 nhóm**.
4. **Nộp Báo cáo & Phân quyền**:
   - Chỉ nộp báo cáo khi đơn đăng ký đã `APPROVED`.
   - Chỉ **Trưởng nhóm** mới được nộp báo cáo tiến độ và nộp các phiên bản mới (`version = latestVersion + 1`).
   - Phân quyền nghiêm ngặt Backend: Truy cập sai vai trò trả về `403 Forbidden`. Tất cả thành viên nhóm, GVHD và Quản lý Khoa có quyền xem lịch sử và tải tệp báo cáo về kiểm tra.
