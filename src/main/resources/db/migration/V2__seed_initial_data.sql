-- V2: Seed initial admin, sample categories, and sample artisan
-- Mật khẩu mặc định cho các tài khoản mẫu: uet123456

-- 1. Admin BQL Làng nghề Bát Tràng
INSERT INTO users (id, email, password_hash, full_name, phone, role, status)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@vcraft.vn',
    '$2a$10$jqmc./tQysLyXTNGg8M4CeIlAO5GffLskOQWPRLjPYfLidU3fVWEO',
    'Ban Quản Lý Làng Nghề Bát Tràng',
    '02438740123',
    'ADMIN',
    'ACTIVE'
);

-- 2. Nghệ nhân mẫu: Xưởng gốm Bát Tràng Cổ
INSERT INTO users (id, email, password_hash, full_name, phone, role, status)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    'artisan@vcraft.vn',
    '$2a$10$jqmc./tQysLyXTNGg8M4CeIlAO5GffLskOQWPRLjPYfLidU3fVWEO',
    'Nghệ nhân Nguyễn Văn Bát',
    '0912345678',
    'ARTISAN',
    'ACTIVE'
);

INSERT INTO artisan_profiles (
    id, user_id, workshop_name, owner_name, phone, address, district,
    latitude, longitude, open_status, description, family_history, status
) VALUES (
    '10000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000002',
    'Xưởng Gốm Bát Tràng Cổ Nghệ Nhân Văn Bát',
    'Nguyễn Văn Bát',
    '0912345678',
    'Số 12, Thôn Giang Cao, Xã Bát Tràng, Gia Lâm, Hà Nội',
    'Bát Tràng',
    20.9785,
    105.9123,
    true,
    'Xưởng chuyên chế tác dòng men lam cổ và men rạn truyền thống từ thế kỷ 16.',
    'Gia tộc 5 đời gìn giữ lò nung củi và kỹ thuật vuốt tay thủ công gia truyền.',
    'APPROVED'
);

-- 3. Khách mua mẫu (Buyer)
INSERT INTO users (id, email, password_hash, full_name, phone, role, status)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    'buyer@vcraft.vn',
    '$2a$10$jqmc./tQysLyXTNGg8M4CeIlAO5GffLskOQWPRLjPYfLidU3fVWEO',
    'Trần Thị Khách',
    '0987654321',
    'BUYER',
    'ACTIVE'
);

-- 4. Danh mục gốm Bát Tràng
INSERT INTO categories (id, name, description, icon) VALUES
    ('20000000-0000-0000-0000-000000000001', 'Bộ Ấm Chén Trà', 'Ấm chén men lam, men rạn, men ngọc cao cấp', 'teapot'),
    ('20000000-0000-0000-0000-000000000002', 'Bình Hoa & Lọ Lộc Bình', 'Lộc bình phong thủy và bình hoa trang trí vuốt tay', 'vase'),
    ('20000000-0000-0000-0000-000000000003', 'Đồ Thờ Cúng Bát Tràng', 'Bát hương, mâm bồng men rạn đắp nổi cổ truyền', 'altar'),
    ('20000000-0000-0000-0000-000000000004', 'Bát Đĩa Bàn Ăn', 'Bộ bát đĩa gốm gia dụng thủ công an toàn cho sức khỏe', 'bowl'),
    ('20000000-0000-0000-0000-000000000005', 'Tranh Gốm & Đồ Mỹ Nghệ', 'Tranh gốm ghép mảnh phù điêu và tượng gốm nghệ thuật', 'art');
