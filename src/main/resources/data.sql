-- ============================================================
-- DATA MẪU KHỞI TẠO (chỉ insert nếu chưa có)
-- ============================================================

-- Admin mặc định
-- Email: admin@bookvehicle.com | Password: Admin@123
INSERT IGNORE INTO users (email, password, phone, role)
VALUES ('admin@bookvehicle.com',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCgAqh5o9z3jpBSb63rye0i',
        '0900000000', 'ADMIN');

-- Ví cho admin
INSERT IGNORE INTO wallets (user_id, balance)
SELECT id, 0.00 FROM users WHERE email = 'admin@bookvehicle.com';

-- Điểm đón thông dụng tại TP.HCM
INSERT IGNORE INTO pickup_points (name, address, latitude, longitude) VALUES
('Sân bay Tân Sơn Nhất',    '60 Trường Sơn, Tân Bình, TP.HCM',        10.8188000, 106.6519000),
('Bến xe Miền Đông',        '292 Đinh Bộ Lĩnh, Bình Thạnh, TP.HCM',  10.8134000, 106.7109000),
('Bến xe Miền Tây',         '395 Kinh Dương Vương, Bình Tân, TP.HCM', 10.7375000, 106.6127000),
('Ga Sài Gòn',              '1 Nguyễn Thông, Q.3, TP.HCM',            10.7800000, 106.6823000),
('Trung tâm TP.HCM - Q.1',  '19 Nguyễn Huệ, Q.1, TP.HCM',            10.7746000, 106.7033000);

-- Giá thuê tài xế mặc định
INSERT IGNORE INTO sober_rates (vehicle_category, hourly_rate, daily_rate) VALUES
('MOTORCYCLE', 50000.00,  400000.00),
('CAR_4',      150000.00, 1000000.00),
('CAR_7',      200000.00, 1500000.00);
