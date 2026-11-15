# vCraft — Nền tảng số Làng nghề Gốm Bát Tràng (Backend)
> **Môn học:** INT3306 - Phát triển ứng dụng web | Fall 2026

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
- JDK 21 trở lên
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

Đọc `AUDIT_VI.md` để xem đối chiếu đề bài, danh sách lỗi và giới hạn kiểm chứng.

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

Tổng số lượng phải bằng yêu cầu gốc, ít nhất hai xưởng đã duyệt khác nhau. Yêu cầu cha chuyển `ALLOCATED`, yêu cầu con `SENT` tới từng xưởng. Buyer duyệt bid và theo dõi giao hàng từng phần. `GET /api/v1/quotes/{id}/allocations` dành cho buyer sở hữu và BQL. Trạng thái cha `ALLOCATED` là nhãn điều phối, không tự tổng hợp trạng thái giao hàng con.

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

Pagination: `page >= 1`, `1 <= limit <= 100`. API nhận enum sai trả 400. `/forgot-password` và `/reset-password` trả 501 vì chưa có luồng email/token reset trong nguồn gốc và nằm ngoài đề bài; không giả báo thành công.
