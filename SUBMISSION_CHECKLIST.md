# SUBMISSION_CHECKLIST

Rà soát tài liệu ngày 18/10/2026 trên nhánh `feature/core-admin`, tại commit [`a95b6fc`](https://github.com/bowhitehat/Group9_24162035_24162053_24162065/commit/a95b6fcf6566d900e722d6db0b0190e6771dd2e9) ngày 11/10/2026.

**Quy cách bài nộp mới nhất được ghi nhận** là mục **Bài nộp cập nhật ngày 11/10/2026** trong [README.md](README.md#bài-nộp-cập-nhật-ngày-11102026), đối chiếu với mục **Kiểm thử và bài nộp** trong [deployment/README.md](deployment/README.md). Đây là căn cứ về quy cách repository ngày 11/10. Đã đối chiếu thêm ZIP người dùng cung cấp có README ngày 12/10 (mục 7); tên Word trong gói mới khác tài liệu repository, chưa có bằng chứng phê duyệt Word hoặc hosting đang hoạt động.

## 1. Tên và cấu trúc bài nộp

- [x] Thống nhất tên ZIP nộp theo hai tài liệu ngày 11/10: **`Group09_24162035_24162053_24162065.zip`**. Không dùng tên cũ `Group9_24162035_24162053_24162065.zip`.
- [ ] Chốt tên hai Word giữa tài liệu repository ngày 11/10 và ZIP/README ngày 12/10. Repository ghi `Group9_24162035_24162053_24162065.docx` và `Group9_baocaokq_duan.docx`; ZIP thực tế dùng `Group09_24162035_24162053_24162065.docx` và `BaoCao_KetQuaDuAn_Nhom09.docx`. Không coi khác tên là thiếu báo cáo; cần đồng bộ hướng dẫn trước khi nộp.
- [x] ZIP được cung cấp có hai Word theo tên tại mục 7, `README.txt`, `TAI_KHOAN_DEMO.txt`, `CHECKLIST_NOP_BAI.txt`; mã nguồn nằm trong `source-code/` có `pom.xml`. Khi chạy Maven, chuyển vào `source-code/` trước.
- [x] `diagrams/` trong ZIP có một ảnh ERD tổng thể, tệp DrawIO, mô hình Workbench và bốn use-case (PNG/DrawIO).
- [ ] Duyệt sự khớp đúng giữa sơ đồ và nội dung/hình trong Word; kiểm tra cấu trúc tệp không thay thế duyệt báo cáo.
- [x] ZIP có hướng dẫn trong `source-code/deployment/`, không có `deployment/` trùng ở gốc hoặc `source-code/docs/`. Các liên kết `docs/` trong README chỉ dùng trên GitHub.
- [x] Kiểm tra danh sách ZIP: không có `.env` thật, `.git`, `.m2`, `target`, `uploads`, `artifacts` hoặc tệp khóa riêng. Cấu hình đọc được dùng biến môi trường/giá trị mẫu; quét mẫu token/khóa không phát hiện bí mật. Các tệp `.env.example` là cấu hình mẫu, được phép giữ.

**Cảnh báo script cũ:** [`tools/package_submission.ps1`](tools/package_submission.ps1) vẫn tạo ZIP `Group9_...`, sao chép Word thành `BaoCao_DoAn.docx`, kèm `source-code/docs/` và `deployment/` ở gốc; không tạo đủ cấu trúc bài nộp ngày 11/10. Tham số `-ReportFile` chỉ chọn Word nguồn, không sửa các khác biệt này. Theo `deployment/README.md`, **không dùng script cũ để thay bài nộp đã kiểm tra**. Chỉ đổi tên ZIP đầu ra cũng không giải quyết được sai cấu trúc.

Tại commit được rà soát, cây repository chỉ có `BaoCao_DoAn.docx`, không có ZIP và hai Word mang tên nộp ở trên; danh sách GitHub Releases cũng trống. ZIP người dùng cung cấp đã được kiểm tra riêng tại mục 7; ZIP đó chưa phải artifact được lưu trong repository/Release.

## 2. Báo cáo Word

- [ ] Duyệt bản Word chương 1–6 cuối cùng trong ZIP sau khi thống nhất tên; ghi người duyệt, ngày duyệt và phiên bản hoặc SHA256.
- [ ] Sửa dòng `Tên tệp nén` trong `BaoCao_KetQuaDuAn_Nhom09.docx`: từ `Group9_24162035_24162053_24162065.zip` thành `Group09_24162035_24162053_24162065.zip`.
- Chưa có bằng chứng phê duyệt Word trong các tài liệu/lịch sử đã đối chiếu. Việc README nêu tên báo cáo hoặc repository có `BaoCao_DoAn.docx` không chứng minh bản Word cuối đã được duyệt.

## 3. Hosting

- [x] Đã có URL demo được ghi trong README và tài liệu triển khai: [Railway /login](https://group9-topic-management-production.up.railway.app/login). Phương án ghi nhận ngày 11/10: Railway + Aiven MySQL + Supabase Storage + Brevo HTTPS API.
- [x] Có biên bản kiểm chứng lịch sử, vượt quá việc chỉ ghi URL: [Railway 10/10](deployment/verification-railway-20261010.txt) ghi nhận mở HTTPS và đăng nhập bốn vai trò; [chuyển dữ liệu 10/10](deployment/migration-verification-20261010.txt) ghi nhận tải báo cáo thật từ cloud có SHA256 trùng nguồn; [email 11/10](deployment/verification-email-20261011.txt) ghi nhận thư phân công phản biện đã tới Inbox. Đây là phạm vi được biên bản ghi nhận tại thời điểm đó.
- [ ] Kiểm tra lại hosting tại thời điểm nộp/báo cáo: `/login`, `/actuator/health`, đăng nhập từng vai trò, quyền truy cập và nộp/tải báo cáo. Ghi thời điểm và kết quả thực tế; lượt rà soát tài liệu này chưa kiểm tra trực tiếp dịch vụ.
- [ ] Xác nhận hạn mức Railway trial/credit và tình trạng Aiven/Supabase/Brevo trước buổi báo cáo. Chưa có căn cứ cam kết hoạt động liên tục hoặc cố định 30 ngày.
- Không tiếp tục dùng lý do cũ “chưa có tài khoản/credential nên chưa có URL”. Cũng không suy từ biên bản 10–11/10 rằng hosting hiện hoạt động hoặc toàn bộ nghiệp vụ cloud đã đạt.

## 4. Kết quả kiểm thử

- [x] Đối chiếu số liệu mới nhất được ghi nhất quán trong README và `deployment/README.md` ngày **11/10/2026**:
  - Java: `mvn -B verify` đạt **238 test**; JUnit/MockMvc dùng H2.
  - JavaScript: `node --test src/test/js/period-form.test.cjs src/test/js/date-form.test.cjs` đạt **35 test**; đây là lệnh riêng với Maven.
  - HTTP trên **MySQL 8.0.46, database QA riêng**: **325/325**; kiểm tra trình duyệt **41 lượt**, không lỗi JavaScript.
- `clean verify` được README ghi là đã đạt trước ca hồi quy cuối; sau đó chạy `verify` cho phần thay đổi. Không ghi nhầm rằng ca cuối đã chạy lại `clean verify`.
- Java/MySQL/trình duyệt là số liệu do tài liệu repository ghi nhận, chưa được chạy lại độc lập trong lượt rà soát này; GitHub Actions không có workflow run để đối chiếu log CI. Riêng 35 test JavaScript đã chạy lại trực tiếp từ ZIP và đạt 35/35 (mục 7). README và Word trong ZIP ghi thêm lượt build Java/JavaScript ngày 12/10, chưa có log Java để xác minh độc lập. Không dùng các số liệu này làm bằng chứng mọi tính năng trên Railway đã đạt.
- Kết quả **177 Java / 24 JavaScript ngày 07/10** trong [docs/system-test-report.md](docs/system-test-report.md#8-hồi-quy-tv2tv3--07102026) là lịch sử, không còn là kết quả gần nhất. Các phần Flyway/“chưa có URL production” của báo cáo cũ cũng chỉ phản ánh phiên kiểm thử cũ.
- Bộ test cuối dùng máy chủ giả lập cho SMTP/Brevo/Supabase; tách biệt với biên bản gửi/nhận email cloud thật ngày 11/10.
- [ ] Chưa xác nhận kiểm thử tải lớn, container end-to-end, toàn bộ trình duyệt hoặc toàn bộ email công bố trên cloud; không đánh dấu hoàn thành khi chưa bổ sung bằng chứng.

## 5. Các hạng mục kỹ thuật đã ghi nhận

- [x] Mã nguồn Java 21 / Spring Boot / Thymeleaf
- [x] Module thành viên 3: phản biện, hội đồng, chấm điểm, thông báo, dashboard
- [x] Data contract `docs/member3-data-contract.md`
- [x] ERD và mô tả cơ sở dữ liệu trong `diagrams/` và `docs/chapter-4.md`
- [x] Schema tạo từ JPA relationship mapping, dữ liệu mẫu và hướng dẫn MySQL Workbench; không dùng migration SQL/Flyway
- [x] Dockerfile, docker-compose, `deployment/README.md`, health check
- [x] Screenshot Chương 5 từ app chạy thật với MySQL

## 6. Căn cứ lịch sử để tránh dùng nhầm

| Mốc | Bằng chứng | Ý nghĩa |
|---|---|---|
| 07/10/2026 — [`0ab1efa`](https://github.com/bowhitehat/Group9_24162035_24162053_24162065/commit/0ab1efa3074cf4f89c9986db54634c9e770514cf) | Lần sửa checklist gần nhất trước lượt rà soát | Checklist cũ chưa phản ánh bàn giao 11/10. |
| 10/10/2026 — [`a779004`](https://github.com/bowhitehat/Group9_24162035_24162053_24162065/commit/a779004811edaf14a50c74394e3c2fac9fd21773) | Ghi nhận triển khai và phạm vi xác minh Railway | Có bằng chứng triển khai lịch sử; không bảo đảm uptime hiện tại. |
| 11/10/2026 — [`a95b6fc`](https://github.com/bowhitehat/Group9_24162035_24162053_24162065/commit/a95b6fcf6566d900e722d6db0b0190e6771dd2e9) | Cập nhật README và `deployment/README.md` với tên ZIP, hai Word, cấu trúc và số liệu kiểm thử mới | Là quy cách bài nộp mới nhất trong nhánh được rà soát; không phải bằng chứng phê duyệt Word hoặc kiểm tra ZIP thực tế. |

## 7. Đối chiếu ZIP thực tế người dùng cung cấp

- Tệp: `Group09_24162035_24162053_24162065.zip`, 7.594.733 byte, 237 mục; SHA256: `0b0b44cad9fa197ac5eeee7fbb74fdab7c631e7704cb7f1c7aa59f1644cc33b9`.
- ZIP kiểm tra CRC đạt, không có đường dẫn thoát thư mục, mục trùng hoặc mã hóa. Chỉ có một thư mục gốc `Group09_24162035_24162053_24162065/`.
- `README.txt` và `source-code/README.md` trong ZIP ghi ngày **12/10/2026**; thống nhất hai tên Word thực tế bên dưới. `source-code/deployment/README.md` vẫn ghi hai tên cũ ngày 11/10.

| Vai trò | Tên trong repository ngày 11/10 | Tên thực tế trong ZIP/README ngày 12/10 |
|---|---|---|
| Báo cáo chính | `Group9_24162035_24162053_24162065.docx` | `Group09_24162035_24162053_24162065.docx` |
| Báo cáo kết quả | `Group9_baocaokq_duan.docx` | `BaoCao_KetQuaDuAn_Nhom09.docx` |

- Cả hai Word có cấu trúc DOCX/XML hợp lệ và CRC đạt; báo cáo chính có nội dung Chương 1–6. Thuộc tính Word lưu 64 trang, nhưng chưa render/duyệt bố cục hoặc xác nhận mục 4.2 thực sự bắt đầu trang 16. Không dùng metadata làm bằng chứng duyệt Word.
- Toàn bộ `src/` khớp cây Git tại `a95b6fc` sau khi chuẩn hóa CRLF/LF. Trong 202 tệp của `source-code/`: 70 khớp byte, 126 chỉ khác xuống dòng, 2 khác nội dung (`README.md`, script đóng gói), 4 tệp triển khai Oracle bổ sung. Không thiếu tệp `src/`, `deployment/` hoặc cấu hình build so với repository.
- Chạy lại `node --test src/test/js/period-form.test.cjs src/test/js/date-form.test.cjs` từ mã giải nén: **35/35 đạt**, không lỗi/bỏ qua. Chưa chạy lại 238 test Java vì môi trường rà soát chỉ có Java 17 và không có Maven, trong khi dự án yêu cầu Java 21.
- Script `source-code/tools/package_submission.ps1` trong ZIP cũng còn dùng tên `Group9_...`, tên Word/cấu trúc cũ và tham chiếu tệp không có trong gói đã giải nén. Không chạy script này để tái tạo bài nộp.

Các sửa còn cần thực hiện trước khi kết luận gói sẵn sàng nộp:

- [ ] Sửa dòng tên ZIP cũ trong phiếu kết quả dự án.
- [ ] Chốt hai tên Word, đồng bộ README repository và `deployment/README.md` với lựa chọn cuối; nếu giữ bản gói ngày 12/10, dùng đúng hai tên Word thực tế ở bảng trên.
- [ ] Đồng bộ các mốc kiểm thử trong tài liệu: kết quả 11/10 và tuyên bố build lại 12/10 là hai lần ghi nhận; bổ sung log nếu muốn xác nhận lần 12/10.
- [ ] Trong `README.txt`, ghi rõ mở terminal tại `source-code/` trước khi chạy `mvn spring-boot:run`.
- [ ] Duyệt Word và kiểm tra hosting trước buổi báo cáo. Các ô chưa đánh dấu trong `CHECKLIST_NOP_BAI.txt` không phải bằng chứng đã hoàn tất.
