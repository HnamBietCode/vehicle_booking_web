-- ============================================================
-- HỆ THỐNG ĐẶT XE & TÀI XẾ - SCHEMA
-- Spring Boot tự động chạy file này khi khởi động
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    phone       VARCHAR(15)  NOT NULL,
    role        ENUM('ADMIN', 'CUSTOMER', 'DRIVER') NOT NULL,
    avatar_url  VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone)
);

CREATE TABLE IF NOT EXISTS customers (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    full_name   VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    membership  ENUM('STANDARD', 'PREMIUM') NOT NULL DEFAULT 'STANDARD',
    premium_exp DATE,

    CONSTRAINT fk_customers_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_customers_user_id UNIQUE (user_id)
);

CREATE TABLE IF NOT EXISTS drivers (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id             BIGINT NOT NULL,
    full_name           VARCHAR(100) NOT NULL,
    cccd                VARCHAR(12)  NOT NULL,
    cccd_image_url      VARCHAR(500),
    cccd_back_url       VARCHAR(500),
    driver_license      VARCHAR(12)  NOT NULL,
    license_image_url   VARCHAR(500),
    license_expiry      DATE NOT NULL,
    vehicle_types       VARCHAR(255) NOT NULL,
    verification_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    is_available        BOOLEAN NOT NULL DEFAULT TRUE,
    approved_at         DATETIME,
    approved_by         BIGINT,
    province            VARCHAR(100),
    district            VARCHAR(100),
    ward                VARCHAR(100),
    last_completed_at   DATETIME,

    CONSTRAINT fk_drivers_user      FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_drivers_approver  FOREIGN KEY (approved_by) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT uq_drivers_user_id   UNIQUE (user_id),
    CONSTRAINT uq_drivers_cccd      UNIQUE (cccd)
);

CREATE TABLE IF NOT EXISTS driver_licenses (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    driver_id       BIGINT NOT NULL,
    license_number  VARCHAR(20) NOT NULL,
    license_class   VARCHAR(10) NOT NULL,
    license_expiry  DATE NOT NULL,
    vehicle_types   VARCHAR(255) NOT NULL,

    CONSTRAINT fk_dl_driver FOREIGN KEY (driver_id) REFERENCES drivers (id) ON DELETE CASCADE,
    INDEX idx_dl_driver (driver_id)
);

CREATE TABLE IF NOT EXISTS user_sessions (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    expires_at  DATETIME NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_sessions_user    (user_id),
    INDEX idx_sessions_expires (expires_at)
);

CREATE TABLE IF NOT EXISTS pickup_points (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(150) NOT NULL,
    address     VARCHAR(300) NOT NULL,
    latitude    DECIMAL(10, 7),
    longitude   DECIMAL(10, 7),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pickup_points_name_address UNIQUE (name, address)
);

CREATE TABLE IF NOT EXISTS vehicles (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    category        ENUM('MOTORCYCLE', 'CAR_4', 'CAR_7') NOT NULL,
    name            VARCHAR(100) NOT NULL,
    license_plate   VARCHAR(15)  NOT NULL,
    color           VARCHAR(50),
    year            YEAR,
    image_url       VARCHAR(500),
    current_address VARCHAR(300),
    price_per_km    DECIMAL(10, 2) NOT NULL,
    price_per_hour  DECIMAL(10, 2) NOT NULL,
    price_per_day   DECIMAL(10, 2) NOT NULL,
    status          ENUM('AVAILABLE', 'ON_TRIP', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    avg_rating      DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    total_trips     INT NOT NULL DEFAULT 0,
    assigned_driver BIGINT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_vehicles_driver FOREIGN KEY (assigned_driver) REFERENCES drivers (id) ON DELETE SET NULL,
    CONSTRAINT uq_vehicles_plate  UNIQUE (license_plate),
    INDEX idx_vehicles_category   (category),
    INDEX idx_vehicles_status     (status),
    INDEX idx_vehicles_cat_stat   (category, status)
);

CREATE TABLE IF NOT EXISTS wallets (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT         NOT NULL,
    balance     DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallets_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_wallets_user UNIQUE (user_id),
    CONSTRAINT chk_wallets_bal CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS transactions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    wallet_id       BIGINT         NOT NULL,
    type            ENUM('DEPOSIT','WITHDRAW','RENTAL_PAYMENT','BOOKING_PAYMENT',
                         'DRIVER_EARNING','SYSTEM_FEE','REFUND','PREMIUM_UPGRADE') NOT NULL,
    amount          DECIMAL(15, 2) NOT NULL,
    balance_before  DECIMAL(15, 2) NOT NULL,
    balance_after   DECIMAL(15, 2) NOT NULL,
    reference_type  ENUM('RENTAL', 'BOOKING', 'MANUAL') NOT NULL,
    reference_id    BIGINT,
    description     VARCHAR(500),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT chk_transactions_amount CHECK (amount > 0),
    INDEX idx_transactions_wallet  (wallet_id),
    INDEX idx_transactions_created (created_at),
    INDEX idx_transactions_ref     (reference_type, reference_id)
);

CREATE TABLE IF NOT EXISTS vehicle_rentals (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id      BIGINT         NOT NULL,
    vehicle_id       BIGINT         NOT NULL,
    driver_id        BIGINT         NOT NULL,
    pickup_point_id  BIGINT,
    pickup_address   VARCHAR(300)   NOT NULL,
    rental_type      ENUM('HOURLY', 'DAILY') NOT NULL,
    planned_start    DATETIME NOT NULL,
    planned_end      DATETIME NOT NULL,
    actual_start     DATETIME,
    actual_end       DATETIME,
    base_price       DECIMAL(10, 2) NOT NULL,
    extra_fee        DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_price      DECIMAL(10, 2) NOT NULL,
    status           ENUM('PENDING','CONFIRMED','ACTIVE','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    cancel_reason    TEXT,
    payment_status   ENUM('PENDING', 'PAID', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    notes            TEXT,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_rentals_customer FOREIGN KEY (customer_id)    REFERENCES customers    (id),
    CONSTRAINT fk_rentals_vehicle  FOREIGN KEY (vehicle_id)     REFERENCES vehicles     (id),
    CONSTRAINT fk_rentals_driver   FOREIGN KEY (driver_id)      REFERENCES drivers      (id),
    CONSTRAINT fk_rentals_pickup   FOREIGN KEY (pickup_point_id) REFERENCES pickup_points (id) ON DELETE SET NULL,
    CONSTRAINT chk_rentals_dates   CHECK (planned_end > planned_start),
    INDEX idx_rentals_customer     (customer_id),
    INDEX idx_rentals_driver       (driver_id),
    INDEX idx_rentals_status       (status),
    INDEX idx_rentals_dates        (planned_start, planned_end, status)
);

CREATE TABLE IF NOT EXISTS driver_bookings (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id      BIGINT         NOT NULL,
    driver_id        BIGINT,
    vehicle_type     ENUM('MOTORCYCLE', 'CAR_4', 'CAR_7') NOT NULL,
    customer_vehicle VARCHAR(100),
    pickup_address   VARCHAR(300)   NOT NULL,
    pickup_lat       DECIMAL(10, 7),
    pickup_lng       DECIMAL(10, 7),
    destination      VARCHAR(300)   NOT NULL,
    dest_lat         DECIMAL(10, 7),
    dest_lng         DECIMAL(10, 7),
    distance_km      DECIMAL(8, 2),
    base_price       DECIMAL(10, 2),
    discount_amount  DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    total_price      DECIMAL(10, 2),
    status           ENUM('FINDING_DRIVER','ACCEPTED','PICKING_UP',
                          'IN_PROGRESS','COMPLETED','CANCELLED','NO_DRIVER') NOT NULL DEFAULT 'FINDING_DRIVER',
    cancel_reason    TEXT,
    payment_status   ENUM('PENDING', 'PAID', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    notes            TEXT,
    accepted_at      DATETIME,
    completed_at     DATETIME,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_bookings_driver   FOREIGN KEY (driver_id)   REFERENCES drivers   (id) ON DELETE SET NULL,
    INDEX idx_bookings_customer     (customer_id),
    INDEX idx_bookings_driver       (driver_id),
    INDEX idx_bookings_status       (status),
    INDEX idx_bookings_created      (created_at)
);

CREATE TABLE IF NOT EXISTS ratings (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    reviewer_id  BIGINT   NOT NULL,
    target_type  ENUM('DRIVER', 'VEHICLE') NOT NULL,
    target_id    BIGINT   NOT NULL,
    ref_type     ENUM('RENTAL', 'BOOKING', 'OTHER') NOT NULL,
    ref_id       BIGINT,
    stars        TINYINT  NOT NULL,
    comment      TEXT,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ratings_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_ratings_stars   CHECK (stars BETWEEN 1 AND 5),
    CONSTRAINT uq_ratings_ref      UNIQUE (reviewer_id, target_type, target_id, ref_type, ref_id),
    INDEX idx_ratings_reviewer     (reviewer_id),
    INDEX idx_ratings_target       (target_type, target_id)
);

CREATE TABLE IF NOT EXISTS daily_revenue (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_date      DATE NOT NULL,
    total_rentals    INT  NOT NULL DEFAULT 0,
    total_bookings   INT  NOT NULL DEFAULT 0,
    rental_revenue   DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    booking_revenue  DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    system_fee       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    driver_payout    DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    total_revenue    DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_daily_rev_date UNIQUE (report_date),
    INDEX idx_daily_rev_date (report_date)
);

CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        TEXT NOT NULL,
    type        ENUM('BOOKING_ASSIGNED','BOOKING_ACCEPTED','TRIP_COMPLETED',
                     'PAYMENT_DONE','DRIVER_APPROVED','DRIVER_REJECTED','GENERAL') NOT NULL,
    ref_type    ENUM('RENTAL', 'BOOKING', 'DRIVER', 'SYSTEM'),
    ref_id      BIGINT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_notif_user    (user_id, is_read),
    INDEX idx_notif_created (created_at)
);

CREATE TABLE IF NOT EXISTS sober_bookings (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id      BIGINT         NOT NULL,
    driver_id        BIGINT,
    vehicle_category ENUM('MOTORCYCLE', 'CAR_4', 'CAR_7') NOT NULL,
    customer_vehicle VARCHAR(100),
    pickup_address   VARCHAR(300)   NOT NULL,
    start_time       DATETIME       NOT NULL,
    duration         INT            NOT NULL,
    duration_unit    ENUM('HOURLY', 'DAILY') NOT NULL,
    notes            TEXT,
    status           ENUM('PENDING', 'ACCEPTED', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    hourly_rate      DECIMAL(10, 2),
    daily_rate       DECIMAL(10, 2),
    total_price      DECIMAL(10, 2),
    actual_start     DATETIME,
    actual_end       DATETIME,
    cancel_reason    TEXT,
    province         VARCHAR(100),
    district         VARCHAR(100),
    ward             VARCHAR(100),
    payment_status   ENUM('PENDING', 'PAID', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_sober_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_sober_driver   FOREIGN KEY (driver_id)   REFERENCES drivers   (id) ON DELETE SET NULL,
    INDEX idx_sober_customer     (customer_id),
    INDEX idx_sober_driver       (driver_id),
    INDEX idx_sober_status       (status)
);

CREATE TABLE IF NOT EXISTS sober_rates (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_category ENUM('MOTORCYCLE', 'CAR_4', 'CAR_7') NOT NULL UNIQUE,
    hourly_rate      DECIMAL(10, 2) NOT NULL,
    daily_rate       DECIMAL(10, 2) NOT NULL,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- VIEWS
-- ============================================================

CREATE OR REPLACE VIEW v_driver_detail AS
SELECT d.id AS driver_id, u.email, u.phone, u.is_active,
       d.full_name, d.cccd, d.driver_license, d.license_expiry,
       d.vehicle_types, d.verification_status, d.rejection_reason,
       d.is_available, d.approved_at, d.cccd_image_url, d.license_image_url
FROM drivers d JOIN users u ON u.id = d.user_id;

CREATE OR REPLACE VIEW v_available_vehicles AS
SELECT v.id, v.category, v.name AS vehicle_name, v.license_plate, v.color,
       v.price_per_km, v.price_per_hour, v.price_per_day, v.avg_rating, v.total_trips,
       d.full_name AS driver_name, u.phone AS driver_phone, d.is_available AS driver_available
FROM vehicles v
LEFT JOIN drivers d ON d.id = v.assigned_driver
LEFT JOIN users   u ON u.id = d.user_id
WHERE v.status = 'AVAILABLE';

CREATE OR REPLACE VIEW v_rental_history AS
SELECT vr.id AS rental_id, c.full_name AS customer_name,
       ve.name AS vehicle_name, ve.category, ve.license_plate,
       dr.full_name AS driver_name, vr.rental_type,
       vr.planned_start, vr.planned_end, vr.actual_end,
       vr.total_price, vr.status, vr.payment_status, vr.created_at
FROM vehicle_rentals vr
JOIN customers c  ON c.id  = vr.customer_id
JOIN vehicles  ve ON ve.id = vr.vehicle_id
JOIN drivers   dr ON dr.id = vr.driver_id;

CREATE OR REPLACE VIEW v_booking_detail AS
SELECT db.id AS booking_id, c.full_name AS customer_name, u_c.phone AS customer_phone,
       d.full_name AS driver_name, u_d.phone AS driver_phone,
       db.vehicle_type, db.pickup_address, db.destination,
       db.distance_km, db.total_price, db.status, db.payment_status,
       db.created_at, db.completed_at
FROM driver_bookings db
JOIN customers c   ON c.id   = db.customer_id
JOIN users     u_c ON u_c.id = c.user_id
LEFT JOIN drivers d    ON d.id   = db.driver_id
LEFT JOIN users   u_d ON u_d.id = d.user_id;

-- ============================================================
-- IDEMPOTENT UPDATES (For existing databases)
-- ============================================================

-- Ensure existing database is updated to the new VARCHAR type for drivers
ALTER TABLE drivers MODIFY COLUMN vehicle_types VARCHAR(255) NOT NULL;

-- Ensure missing columns are added to driver_bookings if the table already existed
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'driver_bookings' AND COLUMN_NAME = 'distance_km' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE driver_bookings ADD COLUMN distance_km DECIMAL(8, 2) AFTER dest_lng', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'driver_bookings' AND COLUMN_NAME = 'base_price' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE driver_bookings ADD COLUMN base_price DECIMAL(10, 2) AFTER distance_km', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'driver_bookings' AND COLUMN_NAME = 'discount_amount' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE driver_bookings ADD COLUMN discount_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER base_price', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'driver_bookings' AND COLUMN_NAME = 'total_price' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE driver_bookings ADD COLUMN total_price DECIMAL(10, 2) AFTER discount_amount', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'driver_bookings' AND COLUMN_NAME = 'payment_status' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE driver_bookings ADD COLUMN payment_status ENUM(\'PENDING\', \'PAID\', \'REFUNDED\') NOT NULL DEFAULT \'PENDING\' AFTER cancel_reason', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'driver_bookings' AND COLUMN_NAME = 'accepted_at' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE driver_bookings ADD COLUMN accepted_at DATETIME AFTER notes', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'driver_bookings' AND COLUMN_NAME = 'completed_at' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE driver_bookings ADD COLUMN completed_at DATETIME AFTER accepted_at', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Ensure missing columns are added to sober_bookings
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'sober_bookings' AND COLUMN_NAME = 'cancel_reason' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE sober_bookings ADD COLUMN cancel_reason TEXT AFTER actual_end', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'sober_bookings' AND COLUMN_NAME = 'hourly_rate' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE sober_bookings ADD COLUMN hourly_rate DECIMAL(10, 2) AFTER status', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'sober_bookings' AND COLUMN_NAME = 'daily_rate' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE sober_bookings ADD COLUMN daily_rate DECIMAL(10, 2) AFTER hourly_rate', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Ensure advanced assignment columns
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'drivers' AND COLUMN_NAME = 'province' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE drivers ADD COLUMN province VARCHAR(100), ADD COLUMN district VARCHAR(100), ADD COLUMN ward VARCHAR(100), ADD COLUMN last_completed_at DATETIME', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'sober_bookings' AND COLUMN_NAME = 'province' AND TABLE_SCHEMA = DATABASE()) = 0, 'ALTER TABLE sober_bookings ADD COLUMN province VARCHAR(100), ADD COLUMN district VARCHAR(100), ADD COLUMN ward VARCHAR(100)', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Drop old unique constraint on driver_license (now using driver_licenses table)
SET @s = (SELECT IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS WHERE TABLE_NAME = 'drivers' AND INDEX_NAME = 'uq_drivers_license' AND TABLE_SCHEMA = DATABASE()) > 0, 'ALTER TABLE drivers DROP INDEX uq_drivers_license', 'SELECT 1'));
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Expand vehicles.category ENUM to include all vehicle types
ALTER TABLE vehicles MODIFY COLUMN category ENUM('MOTORCYCLE','CAR_4','CAR_7','CAR_16','CAR_29','CAR_45','TRUCK_SMALL','TRUCK_MEDIUM','TRUCK_LARGE') NOT NULL;
