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
