# Vehicle Booking System 🚗

Ứng dụng đặt xe & tài xế xây dựng bằng **Spring Boot + MySQL + Thymeleaf**.

---

## ⚡ Bắt đầu nhanh (cho thành viên mới clone về)

### Yêu cầu cài đặt
- Java 17+
- Maven (hoặc dùng `./mvnw` có sẵn)
- **MySQL 8.0+** đang chạy ở localhost:3306

### Bước 1 — Tạo file cấu hình cá nhân

> File `application.properties` đã có sẵn config mặc định (`username=root`, `password=root`).  
> Nếu máy bạn dùng username/password khác, tạo thêm file **`application-local.properties`** để ghi đè (file này **không commit** lên Git):

```properties
# src/main/resources/application-local.properties
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

Sau đó chạy app với profile local:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Bước 2 — Chạy ứng dụng

```bash
./mvnw spring-boot:run
```

**Vậy là xong! 🎉** Spring Boot sẽ tự động:
1. Tạo database `vehicle_booking_db` nếu chưa có
2. Chạy `schema.sql` → tạo toàn bộ 13 bảng (dùng `CREATE TABLE IF NOT EXISTS`, an toàn khi chạy lại)
3. Chạy `data.sql` → insert admin mặc định + pickup points mẫu (dùng `INSERT IGNORE`)

Mở browser: **http://localhost:8080**

---

## 🔑 Tài khoản mặc định

| Role  | Email                    | Password   |
|-------|--------------------------|------------|
| Admin | admin@bookvehicle.com    | Admin@123  |

---

## 📁 Cấu trúc Database

Xem file đầy đủ: [`src/main/resources/schema.sql`](src/main/resources/schema.sql)

| Bảng | Mô tả |
|------|-------|
| `users` | Tài khoản (ADMIN / CUSTOMER / DRIVER) |
| `customers` | Thông tin khách hàng + hạng Premium |
| `drivers` | Hồ sơ tài xế, CCCD, bằng lái |
| `vehicles` | Quản lý xe (3 loại: xe máy, 4 chỗ, 7 chỗ) |
| `wallets` | Ví điện tử — 1 user 1 ví |
| `transactions` | Lịch sử giao dịch |
| `vehicle_rentals` | Đặt thuê xe theo giờ/ngày |
| `driver_bookings` | Đặt tài xế lái hộ |
| `pickup_points` | Điểm đón thông dụng |
| `ratings` | Đánh giá sao sau chuyến |
| `daily_revenue` | Tổng hợp doanh thu theo ngày |
| `notifications` | Thông báo hệ thống |

---

## 👥 Phân công nhóm

| Thành viên | Module |
|------------|--------|
| TV1 (HUY) | Authentication & Profile — `users`, `customers`, `drivers` |
| TV2 (ĐAN) | Vehicle Management — `vehicles`, `ratings` |
| TV3 (DỸ)  | Vehicle Rental — `vehicle_rentals`, `pickup_points` |
| TV4       | Driver Booking — `driver_bookings` |
| TV5       | Admin & Wallet — `wallets`, `transactions`, `daily_revenue` |

---

## 🛠 Lưu ý khi phát triển

- **KHÔNG** thay đổi `spring.jpa.hibernate.ddl-auto` thành `create` hay `create-drop` — sẽ xoá hết data.
- Khi thêm bảng mới: sửa `schema.sql` (dùng `CREATE TABLE IF NOT EXISTS`).
- Khi thêm data mẫu: sửa `data.sql` (dùng `INSERT IGNORE`).
- File `application-local.properties` phải có trong `.gitignore`.
