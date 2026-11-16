# Bổ sung backend — 16/11/2026

Tiếp tục từ commit `87b21d4b9c012c2443679513b37aecc6213280c9` của nhánh `fix/backend-review`. Chỉ thay đổi backend, không xây frontend.

## Những phần đã hoàn thiện thêm

1. **Điều phối sau khi nhận chào giá:** buyer/BQL chọn bid và số lượng từng xưởng. Giữ giá, số ngày và điều khoản của bid; lưu snapshot bid cho từng phần; bảo toàn tổng số lượng. Tất cả transaction/khóa hàng quote, không thể chốt lại hoặc chia tiếp phần con.
2. **Tổng tiến độ:** API tổng hợp số lượng đã nhận, giao, xác nhận, tổng tiền và trạng thái của các phần. Trạng thái cha ALLOCATED thể hiện cấu trúc, `overallStatus` thể hiện tiến độ nghiệp vụ.
3. **Reset mật khẩu thật:** token ngẫu nhiên 256-bit, hash SHA-256 ở DB, 15 phút, dùng một lần. Cooldown một phút; không trả token qua API. Gửi SMTP qua port domain; có Mailpit overlay để thử trên máy.
4. **Thu hồi phiên:** tăng security version khi đổi/reset mật khẩu hoặc khóa user, xác minh version ở JWT filter. Tài khoản được mở lại phải đăng nhập mới. Bản JWT cũ không có claim version được coi là version 0 cho giai đoạn chuyển đổi; chỉ hợp lệ nếu tài khoản vẫn version 0.
5. **Tiền tệ và cập nhật bid:** chặn số tiền gây làm tròn/tràn DECIMAL(15,2), hỗ trợ sửa riêng ghi chú/giá mà giữ số ngày cũ khi không gửi. Đọc bid sau khi khóa quote để tránh dùng snapshot cũ.
6. **Kiểm thử tích hợp và CI:** Testcontainers PostgreSQL 15, các HTTP test nằm trong project, profile Maven `integration`; GitHub Actions chạy JDK21 và cả hai nhóm test. Maven báo rõ khi JDK khác 21. Maven wrapper có quyền thực thi trên Linux; `.gitattributes` quy định xuống dòng.
7. **Migration nối tiếp:** V6 thêm token reset và security version, V7 thêm index truy vấn quote và product/batch. Không sửa V1–V5 đã phát hành.

## Phạm vi kiểm chứng

- `./mvnw -Pintegration clean verify`: gồm unit/API-slice/ArchUnit và các test PostgreSQL HTTP thực tế. Database mới được tạo riêng, không dùng DB triển khai.
- Kiểm tra JSONB, schema validation/Flyway, nhiều xưởng và tổng tiến độ, chốt thầu đồng thời, rollback tồn kho, phân quyền, password reset đồng thời, token hết hạn/dùng lại, lỗi gửi email rollback và thu hồi JWT sau đổi mật khẩu.
- Mailpit Compose được kiểm tra cấu hình. Integration test SMTP gửi link tới Mailpit nội bộ và kiểm tra bắt buộc TLS; không gửi email ra ngoài. API reset test mock delivery để kiểm soát lỗi gửi.
- Kết quả: **89 unit/API-slice test + 11 integration test đều đạt**, không có failure/error/skip.
- Kết quả lần chạy được ghi trong `verification/completion-2026-11-16.json`. Các kết quả/log 15/11 vẫn giữ làm lịch sử, không đại diện cho bản mới.

## Cấu hình còn cần từ người triển khai

- JDK21; Docker để chạy profile integration. Unit test không cần Docker.
- SMTP thực hoặc Mailpit cho reset. Không có cấu hình thì forgot-password trả 503. Chưa thử SMTP của tài khoản người dùng vì chưa được cung cấp thông tin kết nối.
- JWT_SECRET riêng ổn định khi chạy nhiều instance/giữ phiên qua restart; URL public, CORS và lưu trữ uploads phải phù hợp môi trường. Seed account vẫn là tài khoản demo của dự án.
- Password reset gửi thư đồng bộ với transaction và timeout 5 giây cho kết nối/đọc/ghi SMTP. Chưa có outbox/retry worker; nếu SMTP gửi được nhưng commit DB thất bại, liên kết có thể không dùng được và người dùng cần yêu cầu lại.
- Không triển khai hoặc xác nhận vận hành production, không load-test tải lớn hay bảo đảm mọi tổ hợp đầu vào. Frontend và đánh giá giao diện theo đề vẫn ngoài phạm vi.

## Bàn giao Git

Kết nối GitHub của phiên làm việc trả HTTP 403 khi tạo Git tree (`Resource not accessible by integration`), nên không thể xuất bản nhánh trực tiếp. Bản bàn giao có Git bundle chứa toàn bộ project và bốn commit nối tiếp nhánh cũ, để người dùng clone/fetch rồi push bằng tài khoản của mình. File workflow đã có trong source; chưa xác minh lần chạy GitHub Actions từ remote.
