# Không kiêm nhiệm phản biện và hội đồng cùng đề tài

Ngày sửa: 25/10/2026.

## Quy tắc

- Cùng một giảng viên không được làm phản biện và thành viên hội đồng của cùng một đề tài.
- Vẫn được phản biện đề tài A và tham gia hội đồng chấm đề tài B.
- Phân công phản biện đã hủy và hội đồng đã hủy không còn giữ vai trò.
- Hội đồng chưa có đề tài có thể thêm thành viên. Khi gán đề tài, hệ thống kiểm tra tất cả thành viên và từ chối nếu trùng người phản biện.

## Các điểm kiểm tra

`EvaluationScopeService` tập trung luật; Repository lọc theo đề tài/người/trạng thái tại database. Service gọi luật khi thêm thành viên, gán đề tài, phân công hoặc đổi phản biện, kích hoạt/hoàn tất hội đồng, lưu/nộp phiếu và tính/xác nhận/công bố kết quả.

Các thao tác thay đổi vai trò dùng chung khóa ghi của đề tài trong transaction. Khi thêm thành viên vào hội đồng có nhiều đề tài, khóa các đề tài theo thứ tự ID để nhất quán với các luồng quản lý hiện có. Kiểm thử cạnh tranh xác nhận hai request không cùng tạo ra hai vai trò bị cấm.

## Dữ liệu cũ

Không tự xóa phân công, phiếu hay điểm cũ. Trang hội đồng và chi tiết kết quả hiển thị người bị trùng vai trò; chặn ghi thêm phiếu, tính, xác nhận và công bố khi xung đột còn tồn tại. Nếu phiếu đã nộp hoặc hội đồng đã hoạt động, các ràng buộc bảo toàn lịch sử hiện có vẫn áp dụng; cần xử lý dữ liệu cũ theo quyết định riêng, không bỏ khóa hoặc xóa phiếu tự động.

## Kiểm thử

`GradingRoleExclusivityTest` gồm 16 tình huống: hai thứ tự gán vai trò; đổi phản biện; trạng thái hủy; đề tài khác; chặn dữ liệu cũ; giữ phiếu/kết quả; cảnh báo HTML được escape; bốn tình huống cạnh tranh với hai thứ tự giành khóa. Có chế độ chạy trên database MySQL riêng mang tiền tố `group9_qa_`; không dùng database demo cho test tạo/xóa schema.

Bộ dữ liệu của các kiểm thử luồng công bố cũ được tách người phản biện và thành viên hội đồng theo luật mới, vẫn giữ kiểm tra điểm trung bình, khóa, quyền xác nhận, công bố và rollback thông báo.

Kết quả ngày 25/10/2026:

- Toàn bộ 318 kiểm thử Java: 0 lỗi, 0 thất bại (log `D:/demo_proj_web/tmp/role-policy-verify-final-20261025.log`).
- 16 tình huống luật mới chạy riêng trên MySQL 8: 0 lỗi, 0 thất bại (log `D:/demo_proj_web/local-demo-20261025/role-policy-mysql-tests.log`).
- Lượt `verify` đã chạy đạt các test nhưng không đóng gói được do tiến trình Java cũ giữ file JAR trên Windows. Sau khi dừng đúng tiến trình local, chạy lại `package -DskipTests` thành công; không có sửa mã sau lượt test này.
- Đã tạo bản sao lưu database local trước khi đổi bản chạy; không chạy test tạo/xóa schema trên database demo.
- Khởi động lại ứng dụng chính trên `http://localhost:8090`. Kiểm tra HTTP thật với `faculty` và `reviewer1` trên đề tài 124: cảnh báo xuất hiện ở hội đồng/kết quả; POST tính/công bố bị từ chối; cả hai form chấm của người trùng vai trò trả 403. Đối chiếu SHA-256 các phiếu và điểm chi tiết trước/sau: không thay đổi.
