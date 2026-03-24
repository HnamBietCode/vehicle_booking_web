-- ============================================================
-- HỆ THỐNG ĐẶT XE & TÀI XẾ - DATABASE SCHEMA
-- Version: 1.0
-- Date: 2026-03-05
-- Charset: utf8mb4 (hỗ trợ tiếng Việt và emoji)
-- ============================================================

CREATE DATABASE IF NOT EXISTS vehicle_booking_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE vehicle_booking_db;

-- ============================================================
-- MODULE 1: NGƯỜI DÙNG & XÁC THỰC
-- ============================================================

-- Bảng tài khoản chính (tất cả role đều ở đây)
CREATE TABLE users (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,                          -- BCrypt hash
    phone       VARCHAR(15)  NOT NULL,
    role        ENUM('ADMIN', 'CUSTOMER', 'DRIVER') NOT NULL,
    avatar_url  VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,                  -- Khoá tài khoản nếu FALSE
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email  UNIQUE (email),
    CONSTRAINT uq_users_phone  UNIQUE (phone),
    CONSTRAINT chk_users_email CHECK (email REGEXP '^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$'),
    CONSTRAINT chk_users_phone CHECK (phone REGEXP '^[0-9]{9,12}$')
);

-- Chi tiết khách hàng (1-1 với users khi role = CUSTOMER)
CREATE TABLE customers (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    membership  ENUM('STANDARD', 'PREMIUM') NOT NULL DEFAULT 'STANDARD',
    premium_tier ENUM('BRONZE', 'SILVER', 'GOLD'),
    premium_exp DATE,                                           -- Ngày hết hạn Premium (NULL nếu Standard)

    CONSTRAINT fk_customers_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_customers_user_id UNIQUE (user_id),
    CONSTRAINT chk_customers_premium CHECK (
        (membership = 'STANDARD' AND premium_exp IS NULL)
        OR (membership = 'PREMIUM' AND premium_exp IS NOT NULL)
    )
);

-- Chi tiết tài xế (1-1 với users khi role = DRIVER)
CREATE TABLE drivers (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL,
    full_name           VARCHAR(100) NOT NULL,
    cccd                VARCHAR(12)  NOT NULL,                  -- Căn cước công dân (12 số)
    cccd_image_url      VARCHAR(500),                           -- Ảnh CCCD mặt trước
    cccd_back_url       VARCHAR(500),                           -- Ảnh CCCD mặt sau
    driver_license      VARCHAR(12)  NOT NULL,                  -- Số bằng lái
    license_image_url   VARCHAR(500),
    license_expiry      DATE NOT NULL,
    vehicle_types       SET('MOTORCYCLE', 'CAR_4', 'CAR_7') NOT NULL, -- Loại xe được phép lái
    verification_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,                                   -- Lý do từ chối nếu REJECTED
    is_available        BOOLEAN NOT NULL DEFAULT FALSE,         -- Tài xế có đang rảnh không
    approved_at         DATETIME,
    approved_by         BIGINT,                                 -- Admin đã duyệt

    CONSTRAINT fk_drivers_user      FOREIGN KEY (user_id)      REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_drivers_approver  FOREIGN KEY (approved_by)  REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT uq_drivers_user_id   UNIQUE (user_id),
    CONSTRAINT uq_drivers_cccd      UNIQUE (cccd),
    CONSTRAINT uq_drivers_license   UNIQUE (driver_license),
    CONSTRAINT chk_drivers_cccd     CHECK (cccd REGEXP '^[0-9]{12}$'),
    CONSTRAINT chk_drivers_approval CHECK (
        (verification_status = 'REJECTED' AND rejection_reason IS NOT NULL)
        OR verification_status != 'REJECTED'
    ),
    CONSTRAINT chk_drivers_available CHECK (
        -- Chỉ tài xế APPROVED mới được đặt is_available = TRUE
        (is_available = FALSE)
        OR (is_available = TRUE AND verification_status = 'APPROVED')
    )
);

-- Phiên đăng nhập / Refresh token
CREATE TABLE user_sessions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,                          -- Hash của refresh token
    ip_address  VARCHAR(45),                                    -- Hỗ trợ IPv6
    user_agent  VARCHAR(500),
    expires_at  DATETIME NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_sessions_user    (user_id),
    INDEX idx_sessions_expires (expires_at)
);

CREATE TABLE device_tokens (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(500) NOT NULL,
    platform    VARCHAR(50),
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified_at DATETIME,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_device_user_token UNIQUE (user_id, token),
    INDEX idx_device_user (user_id)
);

-- ============================================================
-- MODULE 2: ĐỊA ĐIỂM
-- ============================================================

-- Điểm đón/trả thông dụng (sân bay, bến xe...)
CREATE TABLE pickup_points (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    address     VARCHAR(300) NOT NULL,
    latitude    DECIMAL(10, 7),
    longitude   DECIMAL(10, 7),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_pickup_lat CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT chk_pickup_lng CHECK (longitude BETWEEN -180 AND 180)
);

-- ============================================================
-- MODULE 3: XE CỘ
-- ============================================================

-- Thông tin xe (được quản lý bởi Admin, gán cho Driver)
CREATE TABLE vehicles (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    category        ENUM('MOTORCYCLE', 'CAR_4', 'CAR_7') NOT NULL,
    name            VARCHAR(100) NOT NULL,                      -- VD: Honda Wave, Toyota Innova
    license_plate   VARCHAR(15)  NOT NULL,
    color           VARCHAR(50),
    year            YEAR,
    image_url       VARCHAR(500),
    price_per_km    DECIMAL(10, 2) NOT NULL,                   -- Giá theo km (cho driver booking)
    price_per_hour  DECIMAL(10, 2) NOT NULL,                   -- Giá theo giờ (cho vehicle rental)
    price_per_day   DECIMAL(10, 2) NOT NULL,                   -- Giá theo ngày (cho vehicle rental)
    status          ENUM('AVAILABLE', 'ON_TRIP', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    avg_rating      DECIMAL(3, 2) NOT NULL DEFAULT 0.00,        -- Cached avg rating [0-5]
    total_trips     INT NOT NULL DEFAULT 0,
    assigned_driver BIGINT,                                     -- Tài xế được gán (NULL nếu chưa gán)
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_vehicles_driver    FOREIGN KEY (assigned_driver) REFERENCES drivers (id) ON DELETE SET NULL,
    CONSTRAINT uq_vehicles_plate     UNIQUE (license_plate),
    CONSTRAINT chk_vehicles_rating   CHECK (avg_rating BETWEEN 0.00 AND 5.00),
    CONSTRAINT chk_vehicles_price_km   CHECK (price_per_km  > 0),
    CONSTRAINT chk_vehicles_price_hr   CHECK (price_per_hour > 0),
    CONSTRAINT chk_vehicles_price_day  CHECK (price_per_day  > 0),
    CONSTRAINT chk_vehicles_trips    CHECK (total_trips >= 0),

    INDEX idx_vehicles_category (category),
    INDEX idx_vehicles_status   (status),
    INDEX idx_vehicles_driver   (assigned_driver)
);

-- ============================================================
-- MODULE 4: VÍ ĐIỆN TỬ & GIAO DỊCH
-- ============================================================

-- Ví điện tử (mỗi user có 1 ví)
CREATE TABLE wallets (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT         NOT NULL,
    balance     DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallets_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_wallets_user  UNIQUE (user_id),
    CONSTRAINT chk_wallets_bal  CHECK (balance >= 0)            -- Không cho số dư âm
);

-- Lịch sử giao dịch
CREATE TABLE transactions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_id       BIGINT         NOT NULL,
    type            ENUM(
                        'DEPOSIT',          -- Nạp tiền
                        'WITHDRAW',         -- Rút tiền
                        'RENTAL_PAYMENT',   -- Trả tiền thuê xe
                        'BOOKING_PAYMENT',  -- Trả tiền đặt tài xế
                        'DRIVER_EARNING',   -- Tài xế nhận tiền (80%)
                        'SYSTEM_FEE',       -- Phí hệ thống (20%)
                        'REFUND',           -- Hoàn tiền khi huỷ
                        'PREMIUM_UPGRADE'   -- Phí nâng cấp Premium
                    ) NOT NULL,
    amount          DECIMAL(15, 2) NOT NULL,
    balance_before  DECIMAL(15, 2) NOT NULL,                    -- Số dư trước giao dịch
    balance_after   DECIMAL(15, 2) NOT NULL,                    -- Số dư sau giao dịch
    reference_type  ENUM('RENTAL', 'BOOKING', 'MANUAL') NOT NULL,
    reference_id    BIGINT,                                     -- ID của đơn liên quan
    description     VARCHAR(500),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    CONSTRAINT chk_transactions_bal_after CHECK (balance_after >= 0),

    INDEX idx_transactions_wallet   (wallet_id),
    INDEX idx_transactions_created  (created_at),
    INDEX idx_transactions_ref      (reference_type, reference_id)
);

-- ============================================================
-- MODULE 5: ĐẶT XE (Vehicle Rental - Thuê xe theo giờ/ngày, có tài xế)
-- ============================================================

CREATE TABLE vehicle_rentals (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id         BIGINT         NOT NULL,
    vehicle_id          BIGINT         NOT NULL,
    driver_id           BIGINT,
    pickup_point_id     BIGINT,                                 -- Điểm đón (có thể NULL nếu nhập thủ công)
    pickup_address      VARCHAR(300)   NOT NULL,                -- Địa chỉ đón cụ thể
    rental_type         ENUM('HOURLY', 'DAILY') NOT NULL,
    rental_mode         ENUM('VEHICLE_ONLY', 'WITH_DRIVER') NOT NULL DEFAULT 'WITH_DRIVER',
    planned_start       DATETIME NOT NULL,
    planned_end         DATETIME NOT NULL,
    actual_start        DATETIME,
    actual_end          DATETIME,
    base_price          DECIMAL(10, 2) NOT NULL,               -- Giá cơ bản theo plan
    extra_fee           DECIMAL(10, 2) NOT NULL DEFAULT 0.00,  -- Phí phát sinh (thêm giờ, điểm dừng)
    discount_amount     DECIMAL(10, 2) NOT NULL DEFAULT 0.00,  -- Giảm giá Premium (10%)
    total_price         DECIMAL(10, 2) NOT NULL,
    status              ENUM(
                            'PENDING',      -- Khách vừa đặt, chờ xác nhận
                            'CONFIRMED',    -- Đã xác nhận, tài xế chuẩn bị đến
                            'ACTIVE',       -- Đang trong chuyến
                            'COMPLETED',    -- Hoàn thành
                            'CANCELLED'     -- Đã huỷ
                        ) NOT NULL DEFAULT 'PENDING',
    cancel_reason       TEXT,
    payment_status      ENUM('PENDING', 'PAID', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    notes               TEXT,
    created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_rentals_customer FOREIGN KEY (customer_id)     REFERENCES customers (id),
    CONSTRAINT fk_rentals_vehicle  FOREIGN KEY (vehicle_id)      REFERENCES vehicles  (id),
    CONSTRAINT fk_rentals_driver   FOREIGN KEY (driver_id)       REFERENCES drivers   (id),
    CONSTRAINT fk_rentals_pickup   FOREIGN KEY (pickup_point_id) REFERENCES pickup_points (id) ON DELETE SET NULL,
    CONSTRAINT chk_rentals_dates   CHECK (planned_end > planned_start),
    CONSTRAINT chk_rentals_actual  CHECK (
        actual_end IS NULL OR actual_start IS NULL OR actual_end >= actual_start
    ),
    CONSTRAINT chk_rentals_base    CHECK (base_price     >= 0),
    CONSTRAINT chk_rentals_extra   CHECK (extra_fee      >= 0),
    CONSTRAINT chk_rentals_disc    CHECK (discount_amount >= 0),
    CONSTRAINT chk_rentals_total   CHECK (total_price    >= 0),
    CONSTRAINT chk_rentals_cancel  CHECK (
        (status = 'CANCELLED' AND cancel_reason IS NOT NULL)
        OR status != 'CANCELLED'
    ),

    INDEX idx_rentals_customer (customer_id),
    INDEX idx_rentals_driver   (driver_id),
    INDEX idx_rentals_vehicle  (vehicle_id),
    INDEX idx_rentals_status   (status),
    INDEX idx_rentals_created  (created_at)
);

-- ============================================================
-- MODULE 6: ĐẶT TÀI XẾ LÁI HỘ (Driver Booking)
-- ============================================================

CREATE TABLE driver_bookings (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id     BIGINT         NOT NULL,
    driver_id       BIGINT,                                     -- NULL cho đến khi tài xế nhận chuyến
    vehicle_type    ENUM('MOTORCYCLE', 'CAR_4', 'CAR_7') NOT NULL,
    customer_vehicle VARCHAR(100),                              -- Xe của khách (nếu có ghi chú)
    pickup_address  VARCHAR(300)   NOT NULL,
    pickup_lat      DECIMAL(10, 7),
    pickup_lng      DECIMAL(10, 7),
    destination     VARCHAR(300)   NOT NULL,
    dest_lat        DECIMAL(10, 7),
    dest_lng        DECIMAL(10, 7),
    distance_km     DECIMAL(8, 2),                             -- Khoảng cách (km)
    base_price      DECIMAL(10, 2),                            -- Giá = giá cơ bản + giá/km × km
    discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_price     DECIMAL(10, 2),
    status          ENUM(
                        'FINDING_DRIVER', -- Đang tìm tài xế
                        'ACCEPTED',       -- Tài xế đã nhận
                        'PICKING_UP',     -- Tài xế đang đến điểm đón
                        'IN_PROGRESS',    -- Đang chở khách
                        'COMPLETED',      -- Hoàn thành
                        'CANCELLED',      -- Bị huỷ
                        'NO_DRIVER'       -- Không tìm được tài xế
                    ) NOT NULL DEFAULT 'FINDING_DRIVER',
    cancel_reason   TEXT,
    payment_status  ENUM('PENDING', 'PAID', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    notes           TEXT,
    accepted_at     DATETIME,
    completed_at    DATETIME,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_bookings_driver   FOREIGN KEY (driver_id)   REFERENCES drivers   (id) ON DELETE SET NULL,
    CONSTRAINT chk_bookings_dist    CHECK (distance_km IS NULL OR distance_km > 0),
    CONSTRAINT chk_bookings_base    CHECK (base_price    IS NULL OR base_price    >= 0),
    CONSTRAINT chk_bookings_disc    CHECK (discount_amount >= 0),
    CONSTRAINT chk_bookings_total   CHECK (total_price   IS NULL OR total_price   >= 0),
    CONSTRAINT chk_bookings_cancel  CHECK (
        (status = 'CANCELLED' AND cancel_reason IS NOT NULL)
        OR status != 'CANCELLED'
    ),
    CONSTRAINT chk_bookings_driver_accepted CHECK (
        -- Nếu status là ACCEPTED trở lên, driver_id phải có
        (status IN ('FINDING_DRIVER', 'CANCELLED', 'NO_DRIVER'))
        OR (driver_id IS NOT NULL)
    ),
    CONSTRAINT chk_bookings_lat_pk  CHECK (pickup_lat IS NULL OR pickup_lat BETWEEN -90 AND 90),
    CONSTRAINT chk_bookings_lng_pk  CHECK (pickup_lng IS NULL OR pickup_lng BETWEEN -180 AND 180),
    CONSTRAINT chk_bookings_lat_dt  CHECK (dest_lat   IS NULL OR dest_lat   BETWEEN -90 AND 90),
    CONSTRAINT chk_bookings_lng_dt  CHECK (dest_lng   IS NULL OR dest_lng   BETWEEN -180 AND 180),

    INDEX idx_bookings_customer (customer_id),
    INDEX idx_bookings_driver   (driver_id),
    INDEX idx_bookings_status   (status),
    INDEX idx_bookings_created  (created_at)
);

-- ============================================================
-- MODULE 7: ĐÁNH GIÁ & NHẬN XÉT
-- ============================================================

CREATE TABLE ratings (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    reviewer_id BIGINT    NOT NULL,                             -- User đánh giá (customer)
    target_type ENUM('DRIVER', 'VEHICLE') NOT NULL,
    target_id   BIGINT    NOT NULL,                             -- ID driver hoặc vehicle
    ref_type    ENUM('RENTAL', 'BOOKING') NOT NULL,             -- Đánh giá từ đơn nào
    ref_id      BIGINT    NOT NULL,
    stars       TINYINT   NOT NULL,
    comment     TEXT,
    created_at  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ratings_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_ratings_stars   CHECK (stars BETWEEN 1 AND 5),
    -- Mỗi đơn chỉ được đánh giá 1 lần cho mỗi loại target
    CONSTRAINT uq_ratings_ref      UNIQUE (reviewer_id, target_type, target_id, ref_type, ref_id),

    INDEX idx_ratings_target  (target_type, target_id),
    INDEX idx_ratings_reviewer (reviewer_id)
);

-- ============================================================
-- MODULE 8: THỐNG KÊ & BÁO CÁO (Materialized / Summary tables)
-- ============================================================

-- Bảng tổng hợp doanh thu theo ngày (dành cho Admin report)
CREATE TABLE daily_revenue (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_date         DATE NOT NULL,
    total_rentals       INT  NOT NULL DEFAULT 0,
    total_bookings      INT  NOT NULL DEFAULT 0,
    rental_revenue      DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    booking_revenue     DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    system_fee          DECIMAL(15, 2) NOT NULL DEFAULT 0.00,   -- 20% doanh thu
    driver_payout       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,   -- 80% cho tài xế
    total_revenue       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    updated_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_daily_rev_date UNIQUE (report_date),
    INDEX idx_daily_rev_date (report_date)
);

-- ============================================================
-- MODULE 9: THÔNG BÁO
-- ============================================================

CREATE TABLE notifications (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        TEXT NOT NULL,
    type        VARCHAR(50) NOT NULL,
    /*
    type        ENUM(
                    'BOOKING_ASSIGNED',   -- Tài xế được gán chuyến
                    'BOOKING_ACCEPTED',   -- Khách tìm được tài xế
                    'TRIP_COMPLETED',     -- Hoàn thành chuyến
                    'PAYMENT_DONE',       -- Thanh toán thành công
                    'DRIVER_APPROVED',    -- Tài xế được duyệt
                    'DRIVER_REJECTED',    -- Tài xế bị từ chối
                    'GENERAL'
                ) NOT NULL, */
    ref_type    VARCHAR(50),
    ref_id      BIGINT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_notif_user   (user_id, is_read),
    INDEX idx_notif_created (created_at)
);

-- ============================================================
-- SEED DATA: Dữ liệu khởi tạo ban đầu
-- ============================================================

-- Admin mặc định (password: Admin@123 -> BCrypt)
INSERT INTO users (email, password, phone, role) VALUES
('admin@bookvehicle.com', '$2a$12$exampleBcryptHashHere1234567890123456789012', '0900000000', 'ADMIN');

-- Pickup points thông dụng tại TP.HCM
INSERT INTO pickup_points (name, address, latitude, longitude) VALUES
('Sân bay Tân Sơn Nhất',    '60 Trường Sơn, Tân Bình, TP.HCM',        10.8188000, 106.6519000),
('Bến xe Miền Đông',        '292 Đinh Bộ Lĩnh, Bình Thạnh, TP.HCM',  10.8134000, 106.7109000),
('Bến xe Miền Tây',         '395 Kinh Dương Vương, Bình Tân, TP.HCM', 10.7375000, 106.6127000),
('Ga Sài Gòn',              '1 Nguyễn Thông, Q.3, TP.HCM',            10.7800000, 106.6823000),
('Trung tâm TP.HCM - Q.1',  '19 Nguyễn Huệ, Q.1, TP.HCM',            10.7746000, 106.7033000);

-- ============================================================
-- INDEX BỔ SUNG CHO HIỆU NĂNG
-- ============================================================
CREATE INDEX idx_users_role        ON users   (role, is_active);
CREATE INDEX idx_drivers_status    ON drivers (verification_status, is_available);
CREATE INDEX idx_vehicles_cat_stat ON vehicles(category, status);
CREATE INDEX idx_rentals_dates     ON vehicle_rentals (planned_start, planned_end, status);

-- ============================================================
-- VIEWS HỖ TRỢ TRUY VẤN THƯỜNG DÙNG
-- ============================================================

-- View thông tin đầy đủ tài xế (cho admin duyệt hồ sơ)
CREATE OR REPLACE VIEW v_driver_detail AS
SELECT
    d.id                 AS driver_id,
    u.email,
    u.phone,
    u.is_active,
    d.full_name,
    d.cccd,
    d.driver_license,
    d.license_expiry,
    d.vehicle_types,
    d.verification_status,
    d.rejection_reason,
    d.is_available,
    d.approved_at,
    d.cccd_image_url,
    d.cccd_back_url,
    d.license_image_url
FROM drivers d
JOIN users u ON u.id = d.user_id;

-- View xe khả dụng kèm thông tin tài xế gán
CREATE OR REPLACE VIEW v_available_vehicles AS
SELECT
    v.id,
    v.category,
    v.name          AS vehicle_name,
    v.license_plate,
    v.color,
    v.price_per_km,
    v.price_per_hour,
    v.price_per_day,
    v.avg_rating,
    v.total_trips,
    d.full_name     AS driver_name,
    u.phone         AS driver_phone,
    d.is_available  AS driver_available
FROM vehicles v
LEFT JOIN drivers d ON d.id = v.assigned_driver
LEFT JOIN users   u ON u.id = d.user_id
WHERE v.status = 'AVAILABLE';

-- View lịch sử đặt xe của khách (cho màn My Rentals)
CREATE OR REPLACE VIEW v_rental_history AS
SELECT
    vr.id            AS rental_id,
    c.full_name      AS customer_name,
    ve.name          AS vehicle_name,
    ve.category,
    ve.license_plate,
    dr.full_name     AS driver_name,
    vr.rental_type,
    vr.planned_start,
    vr.planned_end,
    vr.actual_end,
    vr.total_price,
    vr.status,
    vr.payment_status,
    vr.created_at
FROM vehicle_rentals vr
JOIN customers c  ON c.id  = vr.customer_id
JOIN vehicles  ve ON ve.id = vr.vehicle_id
JOIN drivers   dr ON dr.id = vr.driver_id;

-- View chuyến đặt tài xế (cho màn My Bookings và Driver Dashboard)
CREATE OR REPLACE VIEW v_booking_detail AS
SELECT
    db.id             AS booking_id,
    c.full_name       AS customer_name,
    u_c.phone         AS customer_phone,
    d.full_name       AS driver_name,
    u_d.phone         AS driver_phone,
    db.vehicle_type,
    db.pickup_address,
    db.destination,
    db.distance_km,
    db.total_price,
    db.status,
    db.payment_status,
    db.created_at,
    db.completed_at
FROM driver_bookings db
JOIN customers c   ON c.id   = db.customer_id
JOIN users     u_c ON u_c.id = c.user_id
LEFT JOIN drivers d    ON d.id   = db.driver_id
LEFT JOIN users   u_d ON u_d.id = d.user_id;
