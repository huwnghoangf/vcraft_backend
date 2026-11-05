# vCraft Project Guidelines (Backend)

## Giới thiệu & Phạm vi đề tài
- **Đề tài**: Nền tảng số Làng nghề Gốm Bát Tràng (vCraft)
- **Học phần**: INT3306 - Phát triển ứng dụng web (Fall 2026)
- **Kiến trúc**: REST API (Clean Architecture 3 tầng), Database PostgreSQL, Flyway, JWT Authentication, Docker.

## Tech Stack
- Java 21, Spring Boot 3.4.5, Maven.
- PostgreSQL làm cơ sở dữ liệu chính, Hibernate/Spring Data JPA với `ddl-auto: validate`.
- Flyway quản lý database migration (`src/main/resources/db/migration/V1__...`).
- Spring Security: Stateless, JWT Access Token (ngắn hạn) + Refresh Token rotation, mã hóa mật khẩu BCrypt.
- Jakarta Validation kiểm tra request payload, SpringDoc OpenAPI cho tài liệu `/swagger-ui.html`.
- Google ZXing sinh mã QR lô gốm, Thumbnailator tối ưu hóa kích thước ảnh.
- ArchUnit kiểm tra nghiêm ngặt tính toàn vẹn của kiến trúc 3 tầng.

## Quy tắc Kiến trúc 3 tầng (Clean Architecture)
Tuân thủ cấu trúc hướng tâm, phụ thuộc chỉ trỏ vào trong: `API -> Domain -> Infrastructure`.

| Tầng | Package | Cho phép sử dụng | Quy định nghiêm ngặt |
|---|---|---|---|
| **API** | `com.vcraft.api` | Spring Web, Controllers, DTOs, `@RestControllerAdvice`, `CallerResolver` | Chỉ gọi Service của Domain, KHÔNG gọi thẳng Repository của Infrastructure. |
| **Nghiệp vụ (Domain)** | `com.vcraft.domain` | **Pure Java thuần túy**: Entities, Value Objects, Domain Services, Repository Interfaces, Exceptions | **TUYỆT ĐỐI KHÔNG IMPORT** Spring, Hibernate, `jakarta.persistence.*`, `jakarta.servlet.*`. Không dùng `@Service`, `@Transactional`, `@Entity`. Các bean Service được wire trong `config`. |
| **Dữ liệu & Kỹ thuật (Infrastructure)** | `com.vcraft.infrastructure` | Spring Data JPA, `@Entity`, Repository Adapters, Security Filters, QR/Image Storage | Implements interface từ Domain, chuyển đổi Entity <-> Domain Model. |
| **Cấu hình (Config)** | `com.vcraft.config` | Spring Config: SecurityConfig, WebConfig, OpenApiConfig, DomainBeansConfig | Nơi đăng ký các domain services thành Spring Beans. |

`ArchitectureTest` được cấu hình để kiểm tra tự động các quy tắc này.
