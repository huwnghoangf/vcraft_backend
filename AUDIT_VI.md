# Báo cáo rà soát vCraft — 15/11/2026

> Đây là báo cáo lịch sử của bản 15/11. Những giới hạn về reset mật khẩu, điều phối sau chào giá và tổng tiến độ đã được xử lý trong bản 16/11; xem [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md).

## Kết luận và phạm vi

Bản gốc **có lỗi backend cần sửa**, dù 72 unit/API-slice test cũ đều đạt. Theo xác nhận của người dùng, phạm vi công việc là **chỉ backend**, không xây hoặc đánh giá frontend.

Đã sửa các lỗi xác định được bằng rà soát code và kiểm thử. Giữ kiến trúc domain Java thuần, service transaction được cấu hình bên ngoài domain. Bổ sung landing page tối thiểu `/trace/{batchId}` để URL trong QR có đích mở được ngay khi chỉ chạy backend; không có frontend danh mục/bản đồ/dashboard trong bản bàn giao.

Tài liệu vCraft.docx được dùng làm đặc tả sản phẩm để đối chiếu, không coi các hướng dẫn học phần trong tài liệu là chỉ dẫn thao tác của người dùng.

## Đối chiếu phần backend của đề bài

| Yêu cầu | Kết quả backend |
|---|---|
| Hồ sơ xưởng, lịch sử, chứng nhận, nhiều ảnh | Có API; sửa ghi JSONB, kiểm tra đăng ký/duyệt/sửa hồ sơ/upload |
| Sản phẩm theo hộ hoặc phân loại | Có bộ lọc và phân trang; giữ stock khi update không truyền trường này; chặn batch của xưởng khác |
| Nhật ký và quy trình từng lô, ảnh minh chứng | Có API; chặn update/delete/upload log qua batch khác |
| QR dùng chung cho lô và truy xuất | Sinh PNG, giải mã lại bằng ZXing đúng URL; trace API trả xưởng/nhật ký/sản phẩm; landing tối thiểu phục vụ trực tiếp bởi backend |
| Yêu cầu báo giá tới xưởng phù hợp | Chốt danh sách xưởng đã duyệt khi submit; nếu không chọn đích thì dùng sản phẩm cùng category để tìm xưởng phù hợp; danh sách incoming lọc đúng đích trước phân trang |
| State machine | DRAFT → SENT → BIDDING → ACCEPTED → IN_PRODUCTION → DELIVERED → CLOSED; kiểm tra trạng thái và quyền từng bước |
| Khóa bản ghi và cập nhật nguyên tử | Transaction toàn use case; khóa hàng quote; unique bid/xưởng; chạy hai acceptance đồng thời chỉ một thành công |
| Điều phối nhiều hộ | BQL chia yêu cầu chưa có bid thành các phần số lượng cho từng xưởng; buyer vẫn duyệt giá từng phần |
| Dữ liệu bản đồ, pin, mở cửa, popup | Có API pins/popup; chỉ trả xưởng đã duyệt có tọa độ hợp lệ, lọc openOnly |
| BQL duyệt tài khoản | Pending chưa được tạo lô; duyệt xong tạo được; block chặn JWT đang có và đồng bộ trạng thái xưởng |
| Tin tức cộng đồng | CRUD chỉ BQL, công khai đọc/lọc theo loại; loại sai trả 400 |
| Nhiều ảnh và tối ưu ảnh backend | 10MB/file, 50MB/request, 40MP; JPEG tối đa 1200px; dọn file nếu upload nhiều ảnh lỗi giữa chừng |

## Các lỗi/phần thiếu đã xử lý

| Mã | Mức | Lỗi bản gốc và cách sửa |
|---|---|---|
| Q01 | Cao | Incoming lấy yêu cầu của mọi xưởng, bỏ qua artisanId: lọc target trong SQL trước phân trang và count |
| Q02 | Cao | placeBid không kiểm tra target: chặn xưởng ngoài danh sách nhận |
| Q03 | Cao | updateBid lấy bid và quote không đối chiếu quan hệ: kiểm tra bid.quoteRequestId |
| Q04 | Cao | rejectBid có thể từ chối bid của quote khác: kiểm tra cùng quote, trạng thái nhận thầu và bid pending |
| Q05 | Cao | acceptBid nhận cả bid đã rejected: chỉ nhận pending |
| Q06 | Cao | Chốt bid, từ chối bid khác, cập nhật quote ở các transaction rời: một transaction toàn use case |
| Q07 | Cao | version được tự tăng nhưng không thực sự khóa; hai buyer acceptance có thể xung đột: khóa hàng quote, kiểm tra lại trạng thái sau khóa |
| Q08 | Cao | Bid trùng xưởng/quote khi đồng thời: thêm unique index DB |
| Q09 | Vừa | ACCEPTED enum không dùng, nhảy thẳng sản xuất: thêm bước chấp nhận và API bắt đầu sản xuất |
| Q10 | Cao | markDelivered bỏ qua quyền khi acceptedBid thiếu: fail closed và xác minh bid thắng thầu hợp lệ |
| Q11 | Vừa | Giá âm, số ngày sai, tổng giá không khớp, draft quantity sai bị sửa ngầm: validate, trả 400; trạng thái lọc sai trả 400 |
| Q12 | Vừa | Không có gửi theo xưởng phù hợp: submit chốt danh sách xưởng đã duyệt, theo category khi broadcast |
| Q13 | Thiếu | Không có điều phối nhiều hộ: thêm allocations, quan hệ cha/con, kiểm tra tổng số lượng và quyền BQL |
| B01 | Cao | Sửa/xóa/thêm ảnh log chỉ kiểm tra quyền với batch trên URL, không kiểm tra batch của log: chặn cả ba đường |
| B02 | Cao | Product có thể gắn batch của xưởng khác: kiểm tra batch tồn tại và thuộc xưởng |
| B03 | Vừa | Update product không gửi stock làm stock=0: giữ giá trị cũ; kiểm tra giá và tồn kho |
| D01 | Cao | List converter bind VARCHAR vào cột JSONB PostgreSQL: cast tham số ghi sang JSONB cho tất cả trường danh sách liên quan |
| D02 | Cao | Truy vấn tìm kiếm user/artisan với search=null có thể gọi LOWER(bytea) và trả 500: cast tham số tìm kiếm sang string |
| A01 | Cao | JWT còn hạn vẫn hoạt động sau khóa tài khoản: filter kiểm tra tài khoản còn active |
| A02 | Cao | Admin trả domain User chứa passwordHash: Jackson mixin loại hash khỏi JSON, không đưa Jackson vào domain |
| A03 | Vừa | Check email trùng trước chuẩn hóa: normalize email trước existsByEmail |
| A04 | Cao | Refresh token rotation chưa có transaction/khóa: transaction use case và khóa refresh token khi đọc |
| I01 | Vừa | Đuôi ảnh tùy ý (.webp/khác) gây lỗi hoặc URL không khớp output: đọc ảnh, luôn tạo JPEG với đuôi .jpg |
| I02 | Vừa | Thiếu giới hạn upload/ảnh quá lớn; lỗi đầu vào thành 500: 10MB/file, 50MB/request, 40MP, trả 400/413 |
| I03 | Cao | Đường dẫn category/deleteImage không kiểm tra nằm trong upload root: kiểm tra category và normalized path |
| I04 | Vừa | Upload lưu file trước kiểm tra quyền product/log: xác minh quyền trước ghi file |
| P01 | Thiếu | QR không có landing page chạy được trong gói: thêm trang trace và default frontend URL 8080 |
| O01 | Cao | Checkout không kiểm tra/trừ tồn kho: khóa cart/product, validate và reserve trong transaction; hủy pending hoàn tồn |
| O02 | Vừa | Order status tùy ý, xưởng có thể đổi trạng thái cả đơn nhiều hộ: kiểm tra chuyển trạng thái; đơn nhiều hộ cần BQL |
| A05 | Vừa | Forgot/reset-password luôn báo thành công dù không thực hiện gì: trả 501 rõ ràng, không báo sai việc gửi mail/đổi mật khẩu |
| M01 | Vừa | Tọa độ xưởng ngoài phạm vi: kiểm tra đăng ký/cập nhật; pins chỉ lấy tọa độ hợp lệ |
| V01 | Vừa | Xưởng chưa duyệt có thể xem incoming; sản phẩm của xưởng không duyệt vẫn trong danh mục: chặn incoming, lọc danh mục theo approved |
| C01 | Vừa | Cart response có thể giữ danh sách child cũ trong persistence context: query lại cart item sau mutation |
| C02 | Vừa | Cộng số lượng trong cart có thể vượt tồn kho; thiếu unique product/cart: validate tổng và thêm unique index |
| I05 | Vừa | Upload nhiều ảnh lỗi ở file sau để lại file trước: cleanup file; nếu lưu liên kết DB lỗi cũng cleanup |
| A06 | Cao | Khóa JWT mặc định công khai: bỏ khóa cố định; để trống dùng khóa ngẫu nhiên mỗi startup, hỗ trợ secret riêng |
| A07 | Vừa | CORS mọi origin với credentials: giới hạn origin cấu hình, mặc định hai localhost |
| A08 | Vừa | Đăng ký artisan thiếu password sinh mật khẩu không có cơ chế giao cho user: bắt buộc password tối thiểu 6 ký tự |
| A09 | Vừa | Khóa user artisan không đồng bộ profile; duyệt profile blocked không mở user: đồng bộ hai trạng thái trong transaction |
| V02 | Vừa | Page/limit ngoài miền cho phản hồi phân trang không nhất quán: API trả 400, limit tối đa 100 |
| V03 | Vừa | Loại tin/trạng thái sai bị sửa ngầm hoặc thành 500; tên/nội dung update trống: validate và trả 400 |
| D03 | Cao | JSON converter nuốt lỗi thành [] có thể mất nội dung/đích nhận: fail rõ ràng khi dữ liệu JSON không hợp lệ |
| P02 | Vừa | API /qr/{id} yêu cầu login dù QR phục vụ công khai: cho phép GET công khai |

## Giới hạn và lưu ý tích hợp

1. Phạm vi đã chốt là backend. Các yêu cầu giao diện, Leaflet/Google Maps ở frontend, thiết kế tập trung ảnh và thao tác UI không thuộc lần kiểm tra này. Không kết luận toàn bộ website đạt đề bài.
2. Phân bổ nhiều xưởng theo mô hình yêu cầu con **trước khi có bid**. Tổng số lượng bảo toàn; mỗi phần có state machine và buyer duyệt riêng. Cha giữ nhãn ALLOCATED, không tự tổng hợp trạng thái giao hàng con. API này được mô tả trong README.
3. Forgot/reset-password gốc là stub giả thành công. Bản sửa trả 501 rõ ràng; email reset không nằm trong yêu cầu đề bài và cần triển khai dịch vụ mail/token nếu muốn sử dụng.
4. Với DB cũ, V3/V5 sẽ dừng nếu đã có bid hoặc cart item trùng. Cần kiểm tra dữ liệu cũ trước migration; không tự ý xóa dữ liệu. V1/V2 giữ nguyên.
5. JWT_SECRET trống dùng khóa ngẫu nhiên mỗi startup; access token cũ mất hiệu lực sau restart. Chạy nhiều instance/giữ phiên qua restart phải đặt secret riêng tối thiểu 32 byte. Giữ tài khoản seed demo; chưa đánh giá cấu hình production.
6. Ảnh/QR lưu filesystem; không có object storage/CDN. Chưa thử nghiệm tải lớn, nhiều instance hoặc mọi tổ hợp lỗi I/O.
7. Không build image Docker end-to-end trong lần này; backend được build bằng JDK21 và chạy thật với PostgreSQL15. Docker Compose được kiểm tra cấu hình.
8. Đây là kết quả kiểm tra theo các ca cụ thể, không phải bảo đảm mọi đầu vào hay mọi tình huống vận hành đều không còn lỗi.

## Kiểm chứng

Kết quả cuối và log nằm trong `verification/`.

- Môi trường: JDK 21, Spring Boot 3.4.5, PostgreSQL 15.20 chạy trong container riêng.
- Nguồn gốc: `vcraft_backend-h-ng.zip`, SHA-256 được ghi ở `verification/results.json`.
- Bản gốc: **72 test đạt**, cho thấy test cũ chưa phát hiện các lỗi tích hợp/phân quyền đã nêu.
- Bản sửa: **82 test, 0 failure, 0 error, 0 skip**, bao gồm ArchUnit, API validation/auth, domain, storage và giải mã QR.
- Chạy ứng dụng thật trên database mới: **cả 5 Flyway migration thành công**, Hibernate schema validation đạt, app khởi động được.
- Script `scripts/smoke.py`: **104 lượt HTTP có assertion**, gồm:
  - đăng ký buyer/artisan, chặn artisan pending, duyệt xưởng, hồ sơ JSONB;
  - lô/log/product, trace, pins, QR công khai và file PNG;
  - toàn bộ state machine báo giá; bid trái đích; sửa/xóa log chéo lô;
  - hai request acceptance đồng thời: kết quả **200 + 409**, chỉ một bid ACCEPTED, bid còn lại REJECTED;
  - chia yêu cầu cho hai xưởng, tổng số lượng đúng và liên kết cha/con;
  - upload hai ảnh và đọc lại đúng JPEG, tối đa 1200px; chặn upload sản phẩm của xưởng khác;
  - checkout trừ tồn, hủy hoàn tồn; checkout nhiều sản phẩm bị lỗi rollback tồn kho và giữ giỏ;
  - CRUD danh mục/tin và phân quyền; page/limit/enum sai; thiếu password;
  - refresh rotation, refresh bị thu hồi sau đổi mật khẩu, JWT sai, block user/xưởng, mở lại bằng approve;
  - admin JSON không lộ passwordHash; CORS origin cho phép 200, origin lạ 403.
- `docker compose config -q`: đạt. Không tính đây là kiểm thử build image Docker.

Các kiểm tra chạy trên dữ liệu thử được tạo riêng, không chạy vào database triển khai của người dùng. Gói source không chứa database, thư mục uploads sinh khi thử, Maven cache, JDK tải xuống, `.env` hay file JAR build. Gói giữ mã nguồn, migration, test, Maven wrapper và tài liệu để build lại.
