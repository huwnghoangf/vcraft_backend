# vCraft — Nền tảng số Làng nghề Gốm Bát Tràng (Backend)
> **Môn học:** INT3306 - Phát triển ứng dụng web | Fall 2026

> Bản bổ sung 16/11/2026: chia đơn sau chào giá, tổng tiến độ, reset mật khẩu, thu hồi JWT cũ và kiểm thử PostgreSQL tự động. Chi tiết: [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md).

## 1. Giới thiệu tổng quan
vCraft là nền tảng web dành cho Ban quản lý (BQL) làng nghề gốm Bát Tràng. Hệ thống hoạt động như một "Triển lãm số", hỗ trợ:
- **Nghệ nhân / Xưởng gốm (Artisan)**: Kể câu chuyện làm nghề, quản lý lô sản xuất, ghi nhật ký các công đoạn gốm (Nhào đất -> Vuốt -> Trang trí -> Nung -> Thành phẩm), cấp mã QR truy xuất nguồn gốc, chào giá đơn hàng sỉ.
- **Khách hàng / Du khách (Buyer)**: Xem bản đồ số làng nghề với các xưởng mở cửa đón khách, mua sản phẩm, gửi yêu cầu đặt hàng số lượng lớn (Bidding).
- **Ban quản lý (Admin)**: Kiểm duyệt hộ sản xuất, đăng tin tức cộng đồng, theo dõi thống kê làng nghề.

## 2. Kiến trúc & Công nghệ
- **Ngôn ngữ & Framework**: Java 21, Spring Boot 3.4.5
- **Kiến trúc**: Clean Architecture 3 tầng (API $\rightarrow$ Domain $\rightarrow$ Infrastructure), phân tách độc lập và kiểm soát tự động bằng ArchUnit.
- **Cơ sở dữ liệu**: PostgreSQL 15, Flyway migration (`ddl-auto: validate`)
- **Bảo mật**: Spring Security Stateless, JWT Access Token & Refresh Token, BCrypt Password Hasher.
- **Tài liệu API**: OpenAPI 3.0 / Swagger UI tại `/swagger-ui.html`
- **Đóng gói**: Docker & Docker Compose

## 3. Hướng dẫn khởi chạy

### Yêu cầu môi trường
- JDK **21** (Maven kiểm tra phiên bản ngay khi bắt đầu; không dùng JDK 25)
- Docker & Docker Compose (tùy chọn nhưng khuyến nghị)

### Cách 1: Khởi chạy nhanh bằng Docker Compose (Khuyến nghị cho team Frontend)
```bash
docker compose up -d
```
Hệ thống sẽ tự động khởi tạo database PostgreSQL và service backend tại cổng `8080`.
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/api/v1/health`

### Cách 2: Chạy trực tiếp qua Maven
1. Bật database PostgreSQL (tên db: `vcraft_db`, user: `postgres`, password: `postgres`).
2. Chạy backend:
```bash
./mvnw clean spring-boot:run
```

## 4. Tài liệu API Endpoints
Base URL: `/api/v1`
Xem chi tiết và test tương tác trực tiếp tại: `http://localhost:8080/swagger-ui.html`

## Bản rà soát và sửa lỗi 15/11/2026

- Trang truy xuất QR: `http://localhost:8080/trace/{batchId}`
- Mặc định `FRONTEND_URL=http://localhost:8080` để QR trỏ tới trang tích hợp. Nếu dùng frontend riêng, đặt lại biến này.
- Phạm vi bàn giao là backend theo yêu cầu đã làm rõ. API bản đồ trả ghim/popup và trạng thái mở cửa; không xây frontend. Trang trace chỉ là landing page tối thiểu phục vụ QR.

### Luồng báo giá sau sửa

`DRAFT -> SENT -> BIDDING -> ACCEPTED -> IN_PRODUCTION -> DELIVERED -> CLOSED`

Sau khi buyer chấp nhận bid, xưởng thắng thầu gọi `PATCH /api/v1/quotes/{id}/start-production` để bắt đầu sản xuất. Không bỏ qua bước `ACCEPTED` nữa.

BQL chia yêu cầu chưa có chào giá qua:

```http
POST /api/v1/quotes/{id}/allocations
Authorization: Bearer <admin-token>
Content-Type: application/json

[{"artisanId":"<uuid-xuong-1>","quantity":600},{"artisanId":"<uuid-xuong-2>","quantity":400}]
```

Tổng số lượng phải bằng yêu cầu gốc, ít nhất hai xưởng đã duyệt khác nhau. Yêu cầu cha chuyển `ALLOCATED`, yêu cầu con `SENT` tới từng xưởng. Buyer duyệt bid và theo dõi giao hàng từng phần. `GET /api/v1/quotes/{id}/allocations` dành cho buyer sở hữu và BQL. Trạng thái lưu của cha là `ALLOCATED`; API `allocation-progress` tổng hợp trạng thái, số lượng và giá trị từ các phần con.

### Kiểm thử

```bash
./mvnw clean verify
# Sau khi bật app và PostgreSQL riêng để kiểm thử:
# scripts/smoke.py tạo dữ liệu thử; chỉ chạy trên DB thử nghiệm.
python -m pip install pillow
python scripts/smoke.py
```

Flyway V1/V2 giữ nguyên. V3 thêm unique index bid/xưởng; V4 lưu quan hệ điều phối. Với DB cũ có bid trùng, V3 dừng để xử lý dữ liệu có kiểm soát; không tự xóa bid.

### Cấu hình bảo mật

`JWT_SECRET` để trống sẽ dùng khóa ký ngẫu nhiên mỗi lần khởi động. Access token cũ mất hiệu lực sau restart. Đặt secret riêng ít nhất 32 byte để giữ phiên ổn định khi restart/chạy nhiều instance. Bản sửa không còn dùng khóa ký mặc định công khai. `CORS_ALLOWED_ORIGINS` mặc định chỉ cho localhost:3000 và localhost:8080; đặt danh sách origin của ứng dụng khi tích hợp.

Pagination: `page >= 1`, `1 <= limit <= 100`. API nhận enum sai trả 400. `/forgot-password` và `/reset-password` đã triển khai. Nếu chưa cấu hình SMTP, forgot-password trả 503; xem hướng dẫn bên dưới.


## 5. Clone về và chạy kiểm thử

Yêu cầu JDK 21 và mạng để Maven tải thư viện. Không cần cài Maven riêng.

Windows PowerShell:

```powershell
# Điền đúng thư mục JDK 21 đã cài trên máy bạn:
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -version
.\mvnw.cmd clean verify
# Chạy thêm kiểm thử PostgreSQL thật: bật Docker Desktop Linux containers trước.
.\mvnw.cmd -Pintegration clean verify
```

Linux/macOS:

```bash
./mvnw -version
./mvnw clean verify
./mvnw -Pintegration clean verify
```

Lệnh mặc định chạy unit/API-slice test, không cần database đang bật. Profile `integration` dùng Testcontainers 1.21.4 tạo PostgreSQL 15 riêng, chạy Flyway/Hibernate và HTTP API trên cổng ngẫu nhiên, tự dọn container sau test. Docker phải đang chạy; lỗi Docker sẽ làm profile thất bại thay vì lặng lẽ bỏ qua kiểm thử. Không dùng database trong `.env` cho các integration test này. Test API reset dùng mock; test SMTP gửi vào Mailpit nội bộ, không gửi thư ra bên ngoài.

CI `.github/workflows/backend.yml` chạy cả hai nhóm test trên JDK 21 khi push `main`, `master`, `fix/**`, `feat/**` và khi mở PR.

## 6. Chọn nhiều xưởng sau khi đã nhận chào giá

Buyer sở hữu yêu cầu (hoặc BQL) gọi:

```http
POST /api/v1/quotes/{id}/accept-allocations
Authorization: Bearer <buyer-token>
Content-Type: application/json

[{"bidId":"<bid-xuong-1>","quantity":600},{"bidId":"<bid-xuong-2>","quantity":400}]
```

Cần ít nhất hai bid PENDING thuộc đúng yêu cầu, hai xưởng đã duyệt khác nhau, tổng số lượng bằng yêu cầu gốc. Chỉ áp dụng khi yêu cầu còn SENT/BIDDING và chưa quá hạn. Đơn giá và điều khoản được lấy từ bid gốc; người mua chủ động chấp nhận giá theo đơn vị cho lượng phân bổ. Tất cả được ghi trong một transaction. Không tự đổi giá theo số lượng.

Mỗi phần nhận một yêu cầu con và bản sao bid ACCEPTED với `totalPrice = pricePerUnit * quantity`. Xưởng bắt đầu sản xuất qua `start-production`; giao và xác nhận theo các API hiện có. Bid gốc được lưu làm lịch sử, các bid không chọn bị từ chối. Không cho chia tiếp một phần con hoặc điều phối lại yêu cầu đã chốt.

`GET /api/v1/quotes/{id}/allocation-progress` dành cho buyer sở hữu/BQL, trả:

- `overallStatus`: SENT, BIDDING, ACCEPTED, IN_PRODUCTION, DELIVERED hoặc CLOSED, tổng hợp từ các phần.
- `totalQuantity`, `acceptedQuantity`, `deliveredQuantity`, `confirmedQuantity`.
- `acceptedValue`: tổng giá trị các bid đã nhận ở phần con.
- `parts`: danh sách yêu cầu con và trạng thái thực tế.

Một phần bắt đầu/giao hàng thì tổng thể là IN_PRODUCTION; chỉ DELIVERED khi tất cả đã giao, CLOSED khi người mua xác nhận toàn bộ. Trạng thái lưu của yêu cầu cha vẫn là ALLOCATED để nhận biết quan hệ điều phối.

## 7. Đặt lại mật khẩu

Cấu hình SMTP bằng các biến trong `.env.example`: `MAIL_ENABLED=true`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, `SMTP_STARTTLS` và `PASSWORD_RESET_URL`. Không commit `.env`.

```http
POST /api/v1/auth/forgot-password
Content-Type: application/json

{"email":"buyer@example.com"}
```

Trả 202 với cùng thông báo cho tài khoản tồn tại, không tồn tại, bị khóa hoặc trong thời gian chờ. Mỗi tài khoản gửi tối đa một thư/phút. Nếu SMTP chưa cấu hình hoặc lỗi gửi, trả 503; không giả báo gửi thành công. Token 256-bit ngẫu nhiên, hiệu lực 15 phút, một lần; database chỉ lưu SHA-256, API không trả token. Khi gửi yêu cầu mới hợp lệ, token cũ bị vô hiệu hóa.

```http
POST /api/v1/auth/reset-password
Content-Type: application/json

{"token":"<token-trong-email>","newPassword":"MatKhauMoi123!"}
```

Mật khẩu tối thiểu 6 ký tự, tối đa 72 byte UTF-8 theo giới hạn BCrypt. Reset thành công hoặc đổi mật khẩu qua change-password đều vô hiệu hóa refresh token và access JWT đã phát hành trước đó bằng security version; cần đăng nhập lại. Khóa tài khoản cũng tăng version nên mở lại tài khoản không làm JWT cũ sống lại.

Để thử email trên máy, dùng Mailpit (hộp thư nội bộ, không gửi email ra ngoài):

```bash
docker compose -f docker-compose.yml -f docker-compose.mail.yml up --build
```

Mở `http://localhost:8025` để đọc thư. Lấy token trong đường dẫn email rồi gọi reset-password bằng Swagger/Postman; không cần frontend. Overlay chỉ tắt STARTTLS cho Mailpit trong mạng Docker nội bộ. Khi dùng SMTP thực, mặc định yêu cầu STARTTLS.
