# Rà soát kiến trúc, hiệu năng và xử lý đồng thời — 21/10/2026

Phạm vi: xử lý các lỗi tương ứng góp ý của giảng viên trong hệ thống quản lý đề tài;
không áp dụng nghiệp vụ đặt phòng của ví dụ vào đồ án này.

## Các bản vá

- Controller nộp điểm chỉ chuyển DTO tới một nghiệp vụ service. Lưu điểm và nộp
  phiếu nằm trong cùng transaction: nộp thiếu tiêu chí sẽ rollback, không làm mất
  điểm, nhận xét hoặc trạng thái SUBMITTED trước đó. Lưu nháp thiếu điểm vẫn hợp lệ.
- Lọc, đếm và phân trang đề tài/đăng ký thực hiện ở database; giữ đủ quyền của người
  đề xuất, hướng dẫn, phản biện và thành viên hội đồng. Tìm kiếm `%`, `_`, `!` là ký
  tự thông thường, không vô tình biến thành ký tự đại diện SQL.
- Không collection-fetch cùng Pageable hoặc first/limit. Lấy trang có quan hệ to-one
  trước, sau đó tải advisors/members chỉ cho ID thuộc trang đó. Regression test bật
  `hibernate.query.fail_on_pagination_over_collection_fetch=true` để phát hiện tái phạm.
- Tìm người dùng theo vai trò, nhóm của sinh viên, kết quả đã công bố và đếm dashboard
  bằng truy vấn có phạm vi. Danh sách kết quả chọn hội đồng hiện hành tại DB, không
  tải toàn bộ kết quả rồi tìm lồng nhau. Không lộ điểm của hội đồng cũ cho thành viên cũ.
- Tải tiêu chí/điểm theo batch thay vì truy vấn từng tiêu chí hoặc từng phiếu chấm.
  Bộ lọc phản biện giữ đúng trạng thái quá hạn và tổ hợp “chưa nộp điểm”.
- Các thao tác sửa cấu trúc/chuyển trạng thái hội đồng dùng chung khóa dòng hội đồng.
  Không thể thêm quá 5 thành viên hoặc thêm hai chủ tịch khi gửi đồng thời. Khi phải
  khóa nhiều đề tài, dùng thứ tự ID thống nhất.
- Chỉnh nhóm dùng khóa nhóm/người dùng cùng READ_COMMITTED: sinh viên không thể gia
  nhập hai nhóm trong cùng đợt do snapshot cũ của MySQL REPEATABLE_READ.
- Khóa/nộp phiếu và đổi/hủy phản biện khóa đề tài trước, rồi đọc trạng thái hiện tại.
  Không ghi đè SUBMITTED bằng trạng thái đã đọc trước khi chờ khóa. Hủy hội đồng cũng
  nhìn thấy phiếu vừa commit trong lúc chờ khóa đề tài. Từ chối đăng ký dùng cùng khóa
  đăng ký như phê duyệt.

## Kiểm thử

28 regression tests mới nằm trong 5 lớp: QueryPerformanceRegressionTest,
ResultQueryRegressionTest, TransactionConcurrencyRegressionTest,
ConcurrentBallotStateRegressionTest và GroupMembershipConcurrencyRegressionTest.

- Full Maven verify: 266 tests, không failure/error/skip; có bật chế độ cấm phân trang
  trên collection fetch. Không còn cảnh báo HHH90003004 trong lần chạy này.
- Chạy riêng 16 bài kiểm thử transaction/concurrency trên MySQL 8.0.46 thật, database
  QA riêng ở localhost:3311: đạt toàn bộ. Có điều khiển thứ tự hai request bằng latch,
  không chỉ thử gửi tuần tự.
- JavaScript unit tests: 35/35 đạt. Playwright kiểm tra ngày/giờ: thông báo ngay khi
  nhập sai, gợi ý/giới hạn phụ thuộc đợt và chặn submit sai.
- HTTP end-to-end trên MySQL QA: 325/325 đạt. Bao gồm đăng nhập/khóa tài khoản, phân
  quyền/CSRF, đề xuất–duyệt–công bố đề tài, nhóm/đăng ký, upload song song/download,
  phản biện/hội đồng, nộp–khóa–tính–xác nhận–công bố điểm, sinh viên xem đúng kết quả,
  Excel/PDF, thông báo/XSS và dữ liệu đầu vào sai.
- Browser smoke: 41 kiểm tra qua admin, khoa, giảng viên, sinh viên; không có lỗi
  JavaScript hoặc ảnh hỏng. Có xem kết quả đã công bố và dashboard ở viewport mobile.

Các số kiểm tra HTTP/browser và số test Maven là các bộ khác nhau, không phải số
lượng tính năng riêng biệt. Đây không phải phép thử tải hàng triệu bản ghi hoặc
cam kết không còn bất kỳ lỗi nào. Tìm kiếm chứa chuỗi (`%keyword%`) vẫn có thể scan
database; việc chuyển xuống SQL không đồng nghĩa mọi truy vấn đều dùng index scan.

## Tái chạy an toàn

```powershell
mvn -B '-Dspring.config.import=' '-Dapp.mail.enabled=false' '-Dapp.demo-password=' `
  '-Dapp.storage.provider=local' `
  '-Dspring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true' verify
node --test src/test/js/period-form.test.cjs src/test/js/date-form.test.cjs
```

Để chạy 3 lớp concurrency trên MySQL riêng, đặt GROUP9_QA_MYSQL_URL,
GROUP9_QA_MYSQL_USERNAME, GROUP9_QA_MYSQL_PASSWORD trong tiến trình test, rồi chạy
`mvn -Dtest=TransactionConcurrencyRegressionTest,ConcurrentBallotStateRegressionTest,GroupMembershipConcurrencyRegressionTest test`
cùng các tùy chọn tắt import/mail ở trên. URL được guard: chỉ localhost/127.0.0.1
và database tên `group9_qa_*`. CẢNH BÁO: profile test dùng create-drop; tuyệt đối
không trỏ tới database chứa dữ liệu cần giữ. Xóa các biến QA khỏi tiến trình sau test.

Không gửi email thật hoặc ghi vào database Aiven trong kiểm thử này. SMTP/Brevo dùng
các bài kiểm thử giả lập/SMTP cục bộ; kiểm thử gửi thư thật trước đây không được tính
là đã chạy lại. Không thay đổi mật khẩu, dữ liệu demo, Word/diagram hoặc cấu hình bí mật.
Production được cập nhật qua push branch feature/core-admin và Railway auto-deploy;
phải kiểm tra status deploy của chính commit mới và health/login sau deploy, không
chỉ căn cứ việc local đã build thành công.
