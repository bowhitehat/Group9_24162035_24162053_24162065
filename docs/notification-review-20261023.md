# Rà soát thông báo người dùng — 23/10/2026

Nền: commit e604c1c tách chưa mở/quá hạn đăng ký sinh viên. Đợt sửa này mở rộng
thông báo cho các vai trò, không đổi mốc thời gian, quy trình chuyển trạng thái,
quyền truy cập, công thức tính điểm hoặc mã enum lưu trong database.

## Thay đổi

- Tách trạng thái đợt khỏi kiểm tra ngày giờ; phân biệt chưa mở, đã đóng,
  chưa đến giờ và quá hạn khi đề xuất đề tài, tạo/sửa nhóm, đăng ký và duyệt đăng ký.
  Thông báo thời gian có mốc dd/MM/yyyy HH:mm.
- Phản biện phân biệt đợt chưa mở/đã kết thúc, chưa phân công/phân công bị hủy;
  deadline vượt giới hạn hiển thị hạn cuối của đợt. JavaScript dùng cùng ý nghĩa.
- Danh sách phản biện chỉ rõ lý do không thể chấm: bị hủy, quá hạn, phiếu đã khóa,
  hoặc kết quả đã xác nhận/công bố. Đọc phiếu và kết quả theo hai truy vấn batch,
  không thêm truy vấn riêng cho từng hàng. Kiểm tra server vẫn quyết định quyền ghi.
- Nhập điểm sai hiển thị tiêu chí cần sửa và giữ nội dung form; thiếu điểm bắt buộc
  nêu tên tiêu chí. Phiếu không được sửa có giải thích. Lưu nháp không bị HTML
  required của hành động nộp điểm chặn, server vẫn kiểm tra giá trị điểm.
- Tách thông báo xác nhận/công bố thất bại theo hội đồng cũ, phiếu chưa nộp đủ,
  thiếu tiêu chí, phiếu chưa khóa (kèm số lượng), điểm tổng kết cần tính lại.
- Các trường ngày bắt buộc và giới hạn độ dài tài khoản có thông báo tiếng Việt;
  lỗi chuyển kiểu không đưa giá trị nhập, tên lớp Java hoặc chi tiết hệ thống ra ngoài.
- Trang 403 giữ giải thích nghiệp vụ được phép hiển thị, vẫn escape HTML. Lỗi
  từ bộ lọc security không có lý do nghiệp vụ dùng câu hướng dẫn chung.
- Nhãn trạng thái/vai trò hiển thị bằng tiếng Việt, cả kết quả Excel/PDF.
  Chuyển trạng thái đợt bị từ chối có bước tiếp theo hợp lệ; giá trị form và enum
  vẫn là mã cũ. Giữ nguyên thông báo đăng nhập chung để tránh dò tài khoản.

## Kiểm thử trên máy

- Maven verify: **302 lượt, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**.
  Chế độ cấm phân trang collection-fetch vẫn được bật. Có test HTTP/MockMvc
  hiển thị tên tiêu chí, giữ điểm/nhận xét, rollback khi thiếu tiêu chí,
  phiếu khóa và lý do 403. Bao gồm các regression nghiệp vụ/concurrency sẵn có.
- JavaScript unit: **36/36 đạt**.
- Playwright headless với Brave có sẵn, fixture HTML riêng: đạt kiểm tra gợi ý/
  giới hạn ngày, lỗi hiện ngay, chặn submit và phân biệt phản biện chưa mở/đã đóng.
  Không dùng cookies hoặc profile trình duyệt cá nhân cho bài test này.
- Không gửi email thật, không sửa database Aiven hoặc đổi secret. Các test Java
  sử dụng profile kiểm thử/database riêng, tắt import cấu hình local và gửi mail thật.
- Không tính lại bộ 325 HTTP và 41 browser của đợt 21/10 là đã chạy lại đợt này;
  các số đó thuộc báo cáo cũ.

Lệnh tái chạy:

```powershell
mvn -B '-Dspring.config.import=' '-Dapp.mail.enabled=false' '-Dapp.demo-password=' `
  '-Dapp.storage.provider=local' `
  '-Dspring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true' verify
node --test src/test/js/period-form.test.cjs src/test/js/date-form.test.cjs
node src/test/js/date-form.browser.cjs
```

Script browser cần Playwright; có thể đặt BROWSER_EXECUTABLE_PATH tới Chromium/
Chrome/Brave đã cài. Không trỏ các profile test create-drop tới database cần giữ.
Sau push cần xác nhận Railway Active cho chính commit mới và health/login;
build local thành công không tự chứng minh production đã cập nhật.
