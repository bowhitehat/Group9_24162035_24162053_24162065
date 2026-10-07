# Yêu cầu và quyền toàn hệ thống

Hệ thống quản lý vòng đời đề tài từ đợt đăng ký đến công bố điểm. Backend kiểm tra quyền bằng SecurityFilterChain, method security và quan hệ dữ liệu; ẩn menu chỉ hỗ trợ giao diện.

## Ma trận quyền

| Tác nhân nghiệp vụ | Quyền kỹ thuật | Phạm vi hiện tại |
|---|---|---|
| Quản trị | ADMIN | Tài khoản, bộ môn, hỗ trợ đợt và duyệt đăng ký/đề tài; không tự có quyền chấm/công bố điểm |
| Trưởng khoa | FACULTY_MANAGER | Đợt, duyệt đề tài/đăng ký, phản biện, hội đồng, khóa/tính/công bố điểm |
| Trưởng bộ môn | FACULTY_MANAGER dùng chung trong bản hiện tại | Chưa giới hạn độc lập theo bộ môn; cần nhóm/thầy xác nhận có chấp nhận gộp quyền |
| Giảng viên hướng dẫn | LECTURER + quan hệ topic.advisors/proposer | Đề xuất/sửa đề tài được phép, đọc báo cáo liên quan; không chấm đề tài mình hướng dẫn |
| Giảng viên phản biện | LECTURER + ReviewerAssignment | Đọc báo cáo/chấm đúng đề tài được giao và phân công còn hiệu lực |
| Thành viên hội đồng | LECTURER + CouncilMember/CouncilAssignment | Đọc/chấm đề tài hội đồng được giao, đúng trạng thái; CHAIR xác nhận kết quả |
| Trưởng nhóm sinh viên | STUDENT + group.leader | Quản lý thành viên, đăng ký và nộp báo cáo nhóm mình trong hạn |
| Sinh viên thành viên | STUDENT + GroupMember | Xem đăng ký, báo cáo và kết quả đã công bố của nhóm mình |

Chủ tịch/thư ký không phải role tài khoản riêng: CHAIR/SECRETARY/MEMBER nằm trên quan hệ thành viên hội đồng. Trưởng khoa/trưởng bộ môn hiện gộp quyền; không tuyên bố đã phân quyền theo bộ môn.

## Yêu cầu chức năng

| Mã | Yêu cầu | Module |
|---|---|---|
| FR01 | Đăng nhập/đăng xuất, khóa tài khoản, đổi mật khẩu BCrypt | TV1 |
| FR02 | Tạo/xem/sửa tài khoản, vai trò, bộ môn; chống trùng username/email/MSSV/mã bộ môn | TV1 |
| FR03 | Quản lý đợt, trạng thái và lịch GV → SV → báo cáo → phản biện → hội đồng | TV1 |
| FR04 | Đề xuất/sửa/gửi duyệt/duyệt/từ chối/công bố đề tài; chỉ SV thấy đề tài công bố | TV2 |
| FR05 | Lập nhóm, một trưởng nhóm, không ở hai nhóm cùng đợt; cho tạo nhóm thiếu người nhưng bắt buộc 3–5 SV khi đăng ký/duyệt | TV2 đã hoàn thiện |
| FR06 | Đăng ký cùng đợt trong giai đoạn SV; duyệt một đề tài cho một nhóm | TV2 |
| FR07 | Trưởng nhóm nộp trong hạn, lịch sử phiên bản, tải báo cáo theo quan hệ | TV2 |
| FR08 | Phân công phản biện đúng đề tài đã được chấp thuận; theo dõi hạn/phiếu | TV3 |
| FR09 | Hội đồng 3–5 GV, một chủ tịch/thư ký, không trùng, không chấm đề tài đang hướng dẫn | TV3 |
| FR10 | Điểm 0–10, trung bình cộng, đủ điểm, khóa, chủ tịch xác nhận, khoa công bố | TV3 |
| FR11 | SV tra cứu điểm theo đợt đã đăng ký, chỉ xem kết quả nhóm mình sau công bố | TV3 |
| FR12 | Thông báo theo role và dashboard theo phạm vi quyền | TV3 |
| FR13 | Mail công bố cho từng SV và phân công phản biện sau commit, không gửi trùng | TV1 nền SMTP; TV3 nghiệp vụ |
| FR14 | Bộ lọc đề tài kết hợp phân trang | TV2 |
| FR15 | Xuất bảng điểm Excel/PDF đúng quyền và phạm vi | TV3 mở rộng |

## Yêu cầu phi chức năng

| Mã | Điều kiện kiểm tra |
|---|---|
| NFR01 | Chưa đăng nhập chuyển login; sai quyền trả 403; kiểm tra cả POST có CSRF |
| NFR02 | Model/DTO/Controller/Service/Repository tách tầng; không xử lý nghiệp vụ trong template |
| NFR03 | JPA relationship mapping, FK/UK; không SQL/JPQL viết tay; MySQL dev/prod, H2 test |
| NFR04 | Lỗi đầu vào trả form/400, trùng dữ liệu 409, mất file 404, vượt dung lượng 413; không biến thành 500 |
| NFR05 | Mật khẩu BCrypt, CSRF, không ghi secret/file nhạy cảm vào Git hoặc log |
| NFR06 | Transaction cho thao tác ghi, khóa đối tượng khi cần chống duyệt đồng thời |
| NFR07 | UI responsive, menu đúng role, label và phản hồi thao tác; bảng cuộn trên màn hình hẹp |
| NFR08 | SMTP timeout, gửi ngoài transaction; tắt/gửi thất bại không báo đã gửi thành công |
| NFR09 | Test unit/integration và E2E MySQL trước đóng gói; báo cáo đúng lần chạy thực tế |
| NFR10 | Upload ngoài static, bảo vệ đường dẫn; production cần storage bền vững |

## Phân công đến lớp

| Người | Model và repository | DTO và controller | Service và phần chung |
|---|---|---|---|
| Hoàng | Thiết kế/kiểm tra mapping toàn bộ entity; User, Role, Department, RegistrationPeriod, BaseEntity | UserForm, UserUpdateForm, DepartmentForm, PeriodForm; AdminController, FacultyController, GlobalWebController, GlobalExceptionHandler | UserService, DepartmentService, RegistrationPeriodService; SecurityConfig, MailConfig, EmailService, AOP, layout, tích hợp |
| Hưng | Nghiệp vụ Topic, StudentGroup, GroupMember, TopicRegistration, ReportSubmission; mapping phối hợp Hoàng | TopicForm, GroupMemberForm, ReportSubmissionForm, RejectForm; TopicController, StudentGroupController, TopicRegistrationController, ReportSubmissionController | TopicService, StudentGroupService, TopicRegistrationService, ReportSubmissionService; test và UI tương ứng |
| Kiệt | Nghiệp vụ ReviewerAssignment, Council/Member/Assignment, Evaluation/Criterion/Score, TopicResult, Announcement; mapping phối hợp Hoàng | ReviewerAssignmentForm/ChangeForm, Council*Form, EvaluationSubmitDto, AnnouncementForm; ReviewerAssignmentController, CouncilController, EvaluationController, AnnouncementController | ReviewerAssignmentService, CouncilService, EvaluationService, TopicResultService, AnnouncementService, DashboardService; mail nghiệp vụ, export, test/UI, đóng gói |

Mục FR là tiêu chí bàn giao, không phải tuyên bố tất cả chức năng đã hoàn thành. TV2/TV3 cần hoàn thiện các phần đang được giao trước kiểm thử tích hợp cuối.
