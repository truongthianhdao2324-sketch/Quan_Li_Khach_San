CREATE DATABASE IF NOT EXISTS QLKS;
USE QLKS;

-- 1. BẢNG PHÒNG (Thành viên 2)
CREATE TABLE phong (
    so_phong VARCHAR(20) PRIMARY KEY,
    trang_thai VARCHAR(30) NOT NULL, 
    gia_phong DECIMAL(15, 2) NOT NULL,
    loai_phong VARCHAR(50) NOT NULL
);

-- 2. BẢNG KHÁCH HÀNG (Thành viên 1)
CREATE TABLE khach_hang (
    ma_khach_hang INT AUTO_INCREMENT PRIMARY KEY,
    so_giay_to VARCHAR(50) NOT NULL UNIQUE, 
    loai_giay_to VARCHAR(50) NOT NULL,
    ho_ten VARCHAR(100) NOT NULL,
    gioi_tinh VARCHAR(10) NOT NULL,
    so_dien_thoai VARCHAR(20) NOT NULL,                      
    email VARCHAR(100),
    so_phong VARCHAR(20),
    FOREIGN KEY (so_phong) REFERENCES phong(so_phong) ON DELETE SET NULL
);

-- 3. BẢNG NHÂN VIÊN
CREATE TABLE nhan_vien (
    ma_nhan_vien INT AUTO_INCREMENT PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    tuoi INT,
    gioi_tinh VARCHAR(10),
    vi_tri_cong_viec VARCHAR(50) NOT NULL, 
    luong DECIMAL(15,2),
    so_dien_thoai VARCHAR(20),
    email VARCHAR(100),
    so_cccd VARCHAR(50)
);

-- 4. BẢNG TÀI KHOẢN
CREATE TABLE tai_khoan (
    ten_dang_nhap VARCHAR(50) PRIMARY KEY,
    mat_khau VARCHAR(100) NOT NULL,
    vai_tro VARCHAR(30) NOT NULL, 
    email VARCHAR(100)
);

-- 5. BẢNG DỊCH VỤ (Thành viên 3)
CREATE TABLE dich_vu (
    ma_dich_vu INT AUTO_INCREMENT PRIMARY KEY,
    ten_dich_vu VARCHAR(100) NOT NULL,
    gia_dich_vu DECIMAL(15, 2) NOT NULL
);

-- 6. BẢNG ĐẶT PHÒNG / CHECK-IN (Thành viên 5)
CREATE TABLE dat_phong (
    ma_dat_phong INT AUTO_INCREMENT PRIMARY KEY,
    ma_khach_hang INT NOT NULL,
    so_phong VARCHAR(20) NOT NULL,
    ngay_nhan_phong DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_tra_phong DATETIME,
    trang_thai VARCHAR(30) DEFAULT 'Đang lưu trú',
    FOREIGN KEY (ma_khach_hang) REFERENCES khach_hang(ma_khach_hang),
    FOREIGN KEY (so_phong) REFERENCES phong(so_phong)
);

-- 7. BẢNG SỬ DỤNG DỊCH VỤ (Thành viên 3)
CREATE TABLE su_dung_dich_vu (
    id INT AUTO_INCREMENT PRIMARY KEY,
    so_phong VARCHAR(20) NOT NULL,
    so_giay_to VARCHAR(50) NOT NULL,
    ma_dich_vu INT NOT NULL,
    so_luong INT DEFAULT 1,
    thanh_tien DECIMAL(15, 2),
    ngay_su_dung DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ma_dich_vu) REFERENCES dich_vu(ma_dich_vu) ON DELETE CASCADE
);

-- 8. BẢNG HÓA ĐƠN (Thành viên 4)
CREATE TABLE hoa_don (
    ma_hoa_don INT AUTO_INCREMENT PRIMARY KEY,
    ma_dat_phong INT NOT NULL,
    ngay_lap DATETIME DEFAULT CURRENT_TIMESTAMP,
    tien_phong DECIMAL(15,2) NOT NULL,
    tien_dich_vu DECIMAL(15,2) DEFAULT 0,
    tong_tien DECIMAL(15,2) NOT NULL,
    trang_thai VARCHAR(30) DEFAULT 'Đã thanh toán',
    FOREIGN KEY (ma_dat_phong) REFERENCES dat_phong(ma_dat_phong)
);

-- 1. Nạp dữ liệu bảng phong 
INSERT INTO phong (so_phong, trang_thai, gia_phong, loai_phong) VALUES 
('101', 'Đã có khách', 500000, 'Tiêu chuẩn'),
('102', 'Còn trống', 500000, 'Tiêu chuẩn'),
('103', 'Đã có khách', 700000, 'Tiêu chuẩn'),
('104', 'Còn trống', 700000, 'Tiêu chuẩn'),
('105', 'Đang dọn phòng', 500000, 'Tiêu chuẩn'),
('201', 'Đang dọn phòng', 1000000, 'Cao cấp'),
('202', 'Đã có khách', 1000000, 'Cao cấp'),
('203', 'Còn trống', 1200000, 'Cao cấp'),
('204', 'Còn trống', 1200000, 'Cao cấp'),
('205', 'Đã có khách', 1000000, 'Cao cấp'),
('301', 'Còn trống', 1500000, 'Sang trọng'),
('302', 'Đã có khách', 1500000, 'Sang trọng'),
('303', 'Còn trống', 1800000, 'Sang trọng'),
('304', 'Đang dọn phòng', 1800000, 'Sang trọng'),
('305', 'Đã có khách', 1500000, 'Sang trọng'),
('401', 'Còn trống', 2500000, 'VIP'),
('402', 'Đã có khách', 2500000, 'VIP'),
('403', 'Còn trống', 3000000, 'VIP'),
('404', 'Đang dọn phòng', 3000000, 'VIP'),
('501', 'Còn trống', 4500000, 'VIP'),
('502', 'Đã có khách', 5000000, 'VIP');

-- 2. Nạp dữ liệu bảng khach_hang 
INSERT INTO khach_hang (so_giay_to, loai_giay_to, ho_ten, gioi_tinh, so_dien_thoai, email, so_phong) VALUES 
('001203001111', 'CCCD', 'Nguyễn Văn An', 'Nam', '0901122334', 'an.nguyen@email.com', '101'),
('079201002222', 'CCCD', 'Trần Thị Mai', 'Nữ', '0912345678', 'mai.tran@email.com', '103'),
('038198003333', 'CCCD', 'Lê Hoàng Long', 'Nam', '0933445566', 'long.le@email.com', '202'),
('025195004444', 'CCCD', 'Phạm Thị Lan', 'Nữ', '0977889900', 'lan.pham@email.com', '205'),
('031200005555', 'CCCD', 'Hoàng Minh Tuấn', 'Nam', '0944556677', 'tuan.hoang@email.com', '302'),
('049197006666', 'CCCD', 'Vũ Thị Ngọc', 'Nữ', '0966112233', 'ngoc.vu@email.com', '305'),
('036189007777', 'CCCD', 'Đặng Quốc Hưng', 'Nam', '0922334455', 'hung.dang@email.com', '402'),
('082202008888', 'CCCD', 'Bùi Tuyết Nhi', 'Nữ', '0988112233', 'nhi.bui@email.com', '502'),
('064196009999', 'CCCD', 'Ngô Văn Minh', 'Nam', '0909887766', 'minh.ngo@email.com', NULL),
('072199010000', 'CCCD', 'Dương Thảo Nguyên', 'Nữ', '0918223344', 'nguyen.duong@email.com', NULL),
('051201011111', 'CCCD', 'Lý Khánh Hòa', 'Nam', '0937665544', 'hoa.ly@email.com', NULL),
('034200012222', 'CCCD', 'Trịnh Xuân Bách', 'Nam', '0941223344', 'bach.trinh@email.com', NULL),
('075197013333', 'CCCD', 'Đỗ Phương Thảo', 'Nữ', '0963445566', 'thao.do@email.com', NULL),
('080202014444', 'CCCD', 'Nguyễn Đức Anh', 'Nam', '0971556677', 'anh.nguyen@email.com', NULL),
('084198015555', 'CCCD', 'Hà Thị Ánh', 'Nữ', '0982667788', 'anh.ha@email.com', NULL),
('089201016666', 'CCCD', 'Phan Thanh Tùng', 'Nam', '0903778899', 'tung.phan@email.com', NULL),
('091195017777', 'CCCD', 'Cao Hoài Thu', 'Nữ', '0914889900', 'thu.cao@email.com', NULL),
('093200018888', 'CCCD', 'Võ Tấn Phát', 'Nam', '0935990011', 'phat.vo@email.com', NULL),
('095197019999', 'CCCD', 'Mai Trúc Linh', 'Nữ', '0946001122', 'linh.mai@email.com', NULL),
('097202020000', 'CCCD', 'Trương Công Định', 'Nam', '0967112233', 'dinh.truong@email.com', NULL),
('099196021111', 'CCCD', 'Lương Thị Hà', 'Nữ', '0978223344', 'ha.luong@email.com', NULL);

-- 3. Nạp dữ liệu bảng nhan_vien 
INSERT INTO nhan_vien (ho_ten, tuoi, gioi_tinh, vi_tri_cong_viec, luong, so_dien_thoai, email, so_cccd) VALUES
('Nguyễn Thị Hạnh', 26, 'Nữ', 'Lễ Tân', 8500000, '0987654321', 'hanh.nt@hotel.com', '034200112233'),
('Trần Văn Bình', 32, 'Nam', 'Quản Lý', 15000000, '0912233445', 'binh.tv@hotel.com', '079194556677'),
('Lê Thị Thu', 24, 'Nữ', 'Lễ Tân', 8000000, '0932233445', 'thu.lt@hotel.com', '038201998877'),
('Phạm Văn Minh', 29, 'Nam', 'Buồng Phòng', 7000000, '0978112233', 'minh.pv@hotel.com', '025197334455'),
('Hoàng Thị Hoa', 28, 'Nữ', 'Buồng Phòng', 7000000, '0945556677', 'hoa.ht@hotel.com', '031198665544'),
('Đỗ Hoàng Nam', 35, 'Nam', 'Bảo Vệ', 7500000, '0963334455', 'nam.dh@hotel.com', '049191112233'),
('Vũ Kim Anh', 25, 'Nữ', 'Lễ Tân', 8200000, '0901234567', 'anh.vk@hotel.com', '036200223344'),
('Đặng Văn Tuấn', 30, 'Nam', 'Bếp Trưởng', 12000000, '0913234567', 'tuan.dv@hotel.com', '038193445566'),
('Bùi Thị Hải', 27, 'Nữ', 'Kế Toán', 10000000, '0924234567', 'hai.bt@hotel.com', '040200556677'),
('Ngô Tấn Tài', 31, 'Nam', 'Bảo Vệ', 7500000, '0935234567', 'tai.nt@hotel.com', '042194667788'),
('Dương Hồng Ngọc', 23, 'Nữ', 'Lễ Tân', 8000000, '0946234567', 'ngoc.dh@hotel.com', '044201778899'),
('Lý Văn Thanh', 34, 'Nam', 'Kỹ Thuật', 9000000, '0957234567', 'thanh.lv@hotel.com', '046191889900'),
('Trịnh Thảo Nhi', 26, 'Nữ', 'Thu Ngân', 8500000, '0968234567', 'nhi.tt@hotel.com', '048200990011'),
('Mai Quốc Việt', 28, 'Nam', 'Pha Chế', 8000000, '0979234567', 'viet.mq@hotel.com', '050197001122'),
('Hà Minh Triết', 33, 'Nam', 'Bảo Vệ', 7500000, '0980234567', 'triet.hm@hotel.com', '052192112233'),
('Phan Thị Dung', 29, 'Nữ', 'Buồng Phòng', 7000000, '0991234567', 'dung.pt@hotel.com', '054196223344'),
('Cao Văn Hùng', 36, 'Nam', 'Quản Lý Kho', 11000000, '0902234567', 'hung.cv@hotel.com', '056189334455'),
('Võ Thị Loan', 30, 'Nữ', 'Phục Vụ', 6500000, '0913234568', 'loan.vt@hotel.com', '058195445566'),
('Lương Văn Hòa', 27, 'Nam', 'Phục Vụ', 6500000, '0924234568', 'hoa.lv@hotel.com', '060198556677'),
('Trương Thúy Kiều', 25, 'Nữ', 'Lễ Tân', 8200000, '0935234568', 'kieu.tt@hotel.com', '062200667788');

-- 4. Nạp dữ liệu bảng tai_khoan 
INSERT INTO tai_khoan (ten_dang_nhap, mat_khau, vai_tro, email) VALUES
('admin', '123456', 'ADMIN', 'admin@hotel.com'),
('letan', '123456', 'RECEPTIONIST', 'letan@hotel.com'),
('letan1', '123456', 'RECEPTIONIST', 'letan2@hotel.com');

-- 5. Nạp dữ liệu bảng dich_vu
INSERT INTO dich_vu (ten_dich_vu, gia_dich_vu) VALUES
('Nước suối tinh khiết', 20000),
('Nước ngọt có ga', 25000),
('Nước ép cam tươi', 40000),
('Nước ép táo', 40000),
('Nước ép dưa hấu', 45000),
('Cà phê rang xay', 30000),
('Trà thảo mộc', 25000),
('Bia chai lạnh', 35000),
('Rượu vang đỏ (Ly)', 120000),
('Rượu vang trắng (Ly)', 120000),
('Set trái cây nhiệt đới', 80000),
('Ăn sáng tại phòng', 70000),
('Giặt ủi quần áo', 50000),
('Dịch vụ đánh giày', 30000),
('Thuê xe máy (Ngày)', 150000),
('Thuê xe ô tô (Ngày)', 800000),
('Đưa đón sân bay (1 chiều)', 200000),
('Giữ hành lý miễn phí', 0),
('Wifi tốc độ cao miễn phí', 0),
('Bãi đỗ xe miễn phí', 0),
('Massage thư giãn toàn thân', 300000),
('Liệu trình Spa chăm sóc da', 500000),
('Thuê phòng họp (Giờ)', 500000),
('Tour tham quan địa phương', 600000),
('Dịch vụ gọi đồ ăn nhanh', 50000);

-- 6. Nạp dữ liệu bảng dat_phong
INSERT INTO dat_phong (ma_khach_hang, so_phong, ngay_nhan_phong, ngay_tra_phong, trang_thai) VALUES
(1, '101', '2026-10-01 08:00:00', NULL, 'Đang lưu trú'),
(2, '103', '2026-10-02 09:30:00', NULL, 'Đang lưu trú'),
(3, '202', '2026-10-03 14:00:00', NULL, 'Đang lưu trú'),
(4, '205', '2026-10-04 10:15:00', NULL, 'Đang lưu trú'),
(5, '302', '2026-10-05 11:00:00', NULL, 'Đang lưu trú'),
(6, '305', '2026-10-06 12:00:00', NULL, 'Đang lưu trú'),
(7, '402', '2026-10-07 15:30:00', NULL, 'Đang lưu trú'),
(8, '502', '2026-10-08 16:00:00', NULL, 'Đang lưu trú'),
(9, '102', '2026-09-01 08:00:00', '2026-09-03 12:00:00', 'Đã trả phòng'),
(10, '104', '2026-09-02 09:00:00', '2026-09-05 12:00:00', 'Đã trả phòng'),
(11, '201', '2026-09-05 10:00:00', '2026-09-07 12:00:00', 'Đã trả phòng'),
(12, '203', '2026-09-08 14:00:00', '2026-09-10 12:00:00', 'Đã trả phòng'),
(13, '204', '2026-09-10 11:00:00', '2026-09-12 12:00:00', 'Đã trả phòng'),
(14, '301', '2026-09-12 13:00:00', '2026-09-15 12:00:00', 'Đã trả phòng'),
(15, '303', '2026-09-15 15:00:00', '2026-09-18 12:00:00', 'Đã trả phòng'),
(16, '304', '2026-09-18 09:00:00', '2026-09-20 12:00:00', 'Đã trả phòng'),
(17, '401', '2026-09-20 10:00:00', '2026-09-22 12:00:00', 'Đã trả phòng'),
(18, '403', '2026-09-22 14:00:00', '2026-09-25 12:00:00', 'Đã trả phòng'),
(19, '404', '2026-09-25 11:00:00', '2026-09-27 12:00:00', 'Đã trả phòng'),
(20, '501', '2026-09-27 16:00:00', '2026-09-30 12:00:00', 'Đã trả phòng');

-- 7. Nạp dữ liệu bảng su_dung_dich_vu
INSERT INTO su_dung_dich_vu (so_phong, so_giay_to, ma_dich_vu, so_luong, thanh_tien, ngay_su_dung) VALUES
('101', '001203001111', 1, 2, 40000, '2026-10-01 09:00:00'),
('101', '001203001111', 6, 1, 30000, '2026-10-01 10:00:00'),
('103', '079201002222', 2, 3, 75000, '2026-10-02 10:30:00'),
('103', '079201002222', 12, 1, 70000, '2026-10-02 11:00:00'),
('202', '038198003333', 3, 2, 80000, '2026-10-03 15:00:00'),
('202', '038198003333', 13, 1, 50000, '2026-10-03 16:00:00'),
('205', '025195004444', 8, 4, 140000, '2026-10-04 18:00:00'),
('302', '031200005555', 11, 1, 80000, '2026-10-05 14:00:00'),
('305', '049197006666', 21, 1, 300000, '2026-10-06 17:00:00'),
('402', '036189007777', 17, 1, 200000, '2026-10-07 08:00:00'),
('102', '064196009999', 1, 2, 40000, '2026-09-01 10:00:00'),
('104', '072199010000', 2, 2, 50000, '2026-09-02 11:00:00'),
('201', '051201011111', 6, 2, 60000, '2026-09-05 14:00:00'),
('203', '034200012222', 12, 2, 140000, '2026-09-08 09:00:00'),
('204', '075197013333', 15, 1, 150000, '2026-09-10 15:00:00'),
('301', '080202014444', 16, 1, 800000, '2026-09-12 16:00:00'),
('303', '084198015555', 21, 2, 600000, '2026-09-15 18:00:00'),
('304', '089201016666', 13, 2, 100000, '2026-09-18 10:00:00'),
('401', '091195017777', 22, 1, 500000, '2026-09-20 11:00:00'),
('501', '095197019999', 24, 1, 600000, '2026-09-27 17:00:00');

-- 8. Nạp dữ liệu bảng hoa_don (20 bản ghi)
INSERT INTO hoa_don (ma_dat_phong, ngay_lap, tien_phong, tien_dich_vu, tong_tien, trang_thai) VALUES
(9,  '2026-09-03 12:00:00', 1000000, 40000,  1040000, 'Đã thanh toán'),
(10, '2026-09-05 12:00:00', 2100000, 50000,  2150000, 'Đã thanh toán'),
(11, '2026-09-07 12:00:00', 2000000, 60000,  2060000, 'Đã thanh toán'),
(12, '2026-09-10 12:00:00', 2400000, 140000, 2540000, 'Đã thanh toán'),
(13, '2026-09-12 12:00:00', 2400000, 150000, 2550000, 'Đã thanh toán'),
(14, '2026-09-15 12:00:00', 4500000, 800000, 5300000, 'Đã thanh toán'),
(15, '2026-09-18 12:00:00', 5400000, 600000, 6000000, 'Đã thanh toán'),
(16, '2026-09-20 12:00:00', 3600000, 100000, 3700000, 'Đã thanh toán'),
(17, '2026-09-22 12:00:00', 5000000, 500000, 5500000, 'Đã thanh toán'),
(18, '2026-09-25 12:00:00', 9000000, 0,      9000000, 'Đã thanh toán'),
(19, '2026-09-27 12:00:00', 6000000, 0,      6000000, 'Đã thanh toán'),
(20, '2026-09-30 12:00:00', 13500000, 600000, 14100000, 'Đã thanh toán'),
(1,  '2026-10-02 08:00:00', 500000,  70000,  570000,  'Đã thanh toán'),
(2,  '2026-10-03 09:00:00', 700000,  145000, 845000,  'Đã thanh toán'),
(3,  '2026-10-04 10:00:00', 1000000, 130000, 1130000, 'Đã thanh toán'),
(4,  '2026-10-05 11:00:00', 1000000, 140000, 1140000, 'Đã thanh toán'),
(5,  '2026-10-06 12:00:00', 1500000, 80000,  1580000, 'Đã thanh toán'),
(6,  '2026-10-07 13:00:00', 1500000, 300000, 1800000, 'Đã thanh toán'),
(7,  '2026-10-08 14:00:00', 2500000, 200000, 2700000, 'Đã thanh toán'),
(8,  '2026-10-09 15:00:00', 5000000, 0,      5000000, 'Đã thanh toán');