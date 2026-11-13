-- NEW DATABASE ONLY. Run with sqlcmd -v DbName="QLKS_TV3" -b -i sql/01_create_demo.sql
-- Never run this against the group's existing database. No DROP/TRUNCATE commands.
IF DB_ID(N'$(DbName)') IS NOT NULL
    THROW 50001, N'Database đã tồn tại. Chọn tên database thử nghiệm mới.', 1;
GO
CREATE DATABASE [$(DbName)];
GO
USE [$(DbName)];
GO
SET XACT_ABORT ON;
BEGIN TRANSACTION;
-- 1. BẢNG PHÒNG (Thành viên 2)
CREATE TABLE phong (
    so_phong NVARCHAR(20) PRIMARY KEY,
    trang_thai NVARCHAR(30) NOT NULL,
    gia_phong DECIMAL(15, 2) NOT NULL,
    loai_phong NVARCHAR(50) NOT NULL
);

-- 2. BẢNG KHÁCH HÀNG (Thành viên 1)
CREATE TABLE khach_hang (
    ma_khach_hang BIGINT IDENTITY(1,1) PRIMARY KEY,
    so_giay_to NVARCHAR(50) NOT NULL UNIQUE,
    loai_giay_to NVARCHAR(50) NOT NULL,
    ho_ten NVARCHAR(100) NOT NULL,
    gioi_tinh NVARCHAR(10) NOT NULL,
    so_dien_thoai NVARCHAR(20) NOT NULL,
    email NVARCHAR(100),
    so_phong NVARCHAR(20),
    FOREIGN KEY (so_phong) REFERENCES phong(so_phong) ON DELETE SET NULL
);

-- 3. BẢNG NHÂN VIÊN
CREATE TABLE nhan_vien (
    ma_nhan_vien BIGINT IDENTITY(1,1) PRIMARY KEY,
    ho_ten NVARCHAR(100) NOT NULL,
    tuoi INT,
    gioi_tinh NVARCHAR(10),
    vi_tri_cong_viec NVARCHAR(50) NOT NULL,
    luong DECIMAL(15,2),
    so_dien_thoai NVARCHAR(20),
    email NVARCHAR(100),
    so_cccd NVARCHAR(50)
);

-- 4. BẢNG TÀI KHOẢN
CREATE TABLE tai_khoan (
    ten_dang_nhap NVARCHAR(50) PRIMARY KEY,
    mat_khau NVARCHAR(100) NOT NULL,
    vai_tro NVARCHAR(30) NOT NULL,
    email NVARCHAR(100)
);

-- 5. BẢNG DỊCH VỤ (Thành viên 3)
CREATE TABLE dich_vu (
    ma_dich_vu BIGINT IDENTITY(1,1) PRIMARY KEY,
    ten_dich_vu NVARCHAR(100) NOT NULL,
    gia_dich_vu DECIMAL(15, 2) NOT NULL CHECK (gia_dich_vu >= 0),
    dang_kinh_doanh BIT NOT NULL DEFAULT 1
);

-- 6. BẢNG ĐẶT PHÒNG / CHECK-IN (Thành viên 5)
CREATE TABLE dat_phong (
    ma_dat_phong BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_khach_hang BIGINT NOT NULL,
    so_phong NVARCHAR(20) NOT NULL,
    ngay_nhan_phong DATETIME2(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_tra_phong DATETIME2(6),
    trang_thai NVARCHAR(30) DEFAULT N'Đang lưu trú',
    FOREIGN KEY (ma_khach_hang) REFERENCES khach_hang(ma_khach_hang),
    FOREIGN KEY (so_phong) REFERENCES phong(so_phong)
);

-- 7. BẢNG SỬ DỤNG DỊCH VỤ (Thành viên 3)
CREATE TABLE su_dung_dich_vu (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    so_phong NVARCHAR(20) NOT NULL,
    so_giay_to NVARCHAR(50) NOT NULL,
    ma_dich_vu BIGINT NOT NULL,
    so_luong INT NOT NULL DEFAULT 1 CHECK (so_luong BETWEEN 1 AND 999),
    ma_dat_phong BIGINT NULL,
    ten_dich_vu_luc_goi NVARCHAR(100) NULL,
    don_gia_luc_goi DECIMAL(15,2) NULL,
    ma_yeu_cau NVARCHAR(36) NULL,
    nguoi_goi NVARCHAR(50) NULL,
    da_gui_thong_bao BIT NOT NULL DEFAULT 0,
    thanh_tien DECIMAL(15, 2) NOT NULL,
    ngay_su_dung DATETIME2(6) DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ma_dich_vu) REFERENCES dich_vu(ma_dich_vu) ON DELETE NO ACTION,
    CONSTRAINT fk_sddv_luot_o FOREIGN KEY (ma_dat_phong) REFERENCES dat_phong(ma_dat_phong)
);

-- 8. BẢNG HÓA ĐƠN (Thành viên 4)
CREATE TABLE hoa_don (
    ma_hoa_don BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_dat_phong BIGINT NOT NULL,
    ngay_lap DATETIME2(6) DEFAULT CURRENT_TIMESTAMP,
    tien_phong DECIMAL(15,2) NOT NULL,
    tien_dich_vu DECIMAL(15,2) DEFAULT 0,
    tong_tien DECIMAL(15,2) NOT NULL,
    trang_thai NVARCHAR(30) DEFAULT N'Đã thanh toán',
    FOREIGN KEY (ma_dat_phong) REFERENCES dat_phong(ma_dat_phong)
);

-- 1. Nạp dữ liệu bảng phong
INSERT INTO phong (so_phong, trang_thai, gia_phong, loai_phong) VALUES
(N'101', N'Đã có khách', 500000, N'Tiêu chuẩn'),
(N'102', N'Còn trống', 500000, N'Tiêu chuẩn'),
(N'103', N'Đã có khách', 700000, N'Tiêu chuẩn'),
(N'104', N'Còn trống', 700000, N'Tiêu chuẩn'),
(N'105', N'Đang dọn phòng', 500000, N'Tiêu chuẩn'),
(N'201', N'Đang dọn phòng', 1000000, N'Cao cấp'),
(N'202', N'Đã có khách', 1000000, N'Cao cấp'),
(N'203', N'Còn trống', 1200000, N'Cao cấp'),
(N'204', N'Còn trống', 1200000, N'Cao cấp'),
(N'205', N'Đã có khách', 1000000, N'Cao cấp'),
(N'301', N'Còn trống', 1500000, N'Sang trọng'),
(N'302', N'Đã có khách', 1500000, N'Sang trọng'),
(N'303', N'Còn trống', 1800000, N'Sang trọng'),
(N'304', N'Đang dọn phòng', 1800000, N'Sang trọng'),
(N'305', N'Đã có khách', 1500000, N'Sang trọng'),
(N'401', N'Còn trống', 2500000, N'VIP'),
(N'402', N'Đã có khách', 2500000, N'VIP'),
(N'403', N'Còn trống', 3000000, N'VIP'),
(N'404', N'Đang dọn phòng', 3000000, N'VIP'),
(N'501', N'Còn trống', 4500000, N'VIP'),
(N'502', N'Đã có khách', 5000000, N'VIP');

-- 2. Nạp dữ liệu bảng khach_hang
INSERT INTO khach_hang (so_giay_to, loai_giay_to, ho_ten, gioi_tinh, so_dien_thoai, email, so_phong) VALUES
(N'001203001111', N'CCCD', N'Nguyễn Văn An', N'Nam', N'0901122334', N'an.nguyen@email.com', N'101'),
(N'079201002222', N'CCCD', N'Trần Thị Mai', N'Nữ', N'0912345678', N'mai.tran@email.com', N'103'),
(N'038198003333', N'CCCD', N'Lê Hoàng Long', N'Nam', N'0933445566', N'long.le@email.com', N'202'),
(N'025195004444', N'CCCD', N'Phạm Thị Lan', N'Nữ', N'0977889900', N'lan.pham@email.com', N'205'),
(N'031200005555', N'CCCD', N'Hoàng Minh Tuấn', N'Nam', N'0944556677', N'tuan.hoang@email.com', N'302'),
(N'049197006666', N'CCCD', N'Vũ Thị Ngọc', N'Nữ', N'0966112233', N'ngoc.vu@email.com', N'305'),
(N'036189007777', N'CCCD', N'Đặng Quốc Hưng', N'Nam', N'0922334455', N'hung.dang@email.com', N'402'),
(N'082202008888', N'CCCD', N'Bùi Tuyết Nhi', N'Nữ', N'0988112233', N'nhi.bui@email.com', N'502'),
(N'064196009999', N'CCCD', N'Ngô Văn Minh', N'Nam', N'0909887766', N'minh.ngo@email.com', NULL),
(N'072199010000', N'CCCD', N'Dương Thảo Nguyên', N'Nữ', N'0918223344', N'nguyen.duong@email.com', NULL),
(N'051201011111', N'CCCD', N'Lý Khánh Hòa', N'Nam', N'0937665544', N'hoa.ly@email.com', NULL),
(N'034200012222', N'CCCD', N'Trịnh Xuân Bách', N'Nam', N'0941223344', N'bach.trinh@email.com', NULL),
(N'075197013333', N'CCCD', N'Đỗ Phương Thảo', N'Nữ', N'0963445566', N'thao.do@email.com', NULL),
(N'080202014444', N'CCCD', N'Nguyễn Đức Anh', N'Nam', N'0971556677', N'anh.nguyen@email.com', NULL),
(N'084198015555', N'CCCD', N'Hà Thị Ánh', N'Nữ', N'0982667788', N'anh.ha@email.com', NULL),
(N'089201016666', N'CCCD', N'Phan Thanh Tùng', N'Nam', N'0903778899', N'tung.phan@email.com', NULL),
(N'091195017777', N'CCCD', N'Cao Hoài Thu', N'Nữ', N'0914889900', N'thu.cao@email.com', NULL),
(N'093200018888', N'CCCD', N'Võ Tấn Phát', N'Nam', N'0935990011', N'phat.vo@email.com', NULL),
(N'095197019999', N'CCCD', N'Mai Trúc Linh', N'Nữ', N'0946001122', N'linh.mai@email.com', NULL),
(N'097202020000', N'CCCD', N'Trương Công Định', N'Nam', N'0967112233', N'dinh.truong@email.com', NULL),
(N'099196021111', N'CCCD', N'Lương Thị Hà', N'Nữ', N'0978223344', N'ha.luong@email.com', NULL);

-- 3. Nạp dữ liệu bảng nhan_vien
INSERT INTO nhan_vien (ho_ten, tuoi, gioi_tinh, vi_tri_cong_viec, luong, so_dien_thoai, email, so_cccd) VALUES
(N'Nguyễn Thị Hạnh', 26, N'Nữ', N'Lễ Tân', 8500000, N'0987654321', N'hanh.nt@hotel.com', N'034200112233'),
(N'Trần Văn Bình', 32, N'Nam', N'Quản Lý', 15000000, N'0912233445', N'binh.tv@hotel.com', N'079194556677'),
(N'Lê Thị Thu', 24, N'Nữ', N'Lễ Tân', 8000000, N'0932233445', N'thu.lt@hotel.com', N'038201998877'),
(N'Phạm Văn Minh', 29, N'Nam', N'Buồng Phòng', 7000000, N'0978112233', N'minh.pv@hotel.com', N'025197334455'),
(N'Hoàng Thị Hoa', 28, N'Nữ', N'Buồng Phòng', 7000000, N'0945556677', N'hoa.ht@hotel.com', N'031198665544'),
(N'Đỗ Hoàng Nam', 35, N'Nam', N'Bảo Vệ', 7500000, N'0963334455', N'nam.dh@hotel.com', N'049191112233'),
(N'Vũ Kim Anh', 25, N'Nữ', N'Lễ Tân', 8200000, N'0901234567', N'anh.vk@hotel.com', N'036200223344'),
(N'Đặng Văn Tuấn', 30, N'Nam', N'Bếp Trưởng', 12000000, N'0913234567', N'tuan.dv@hotel.com', N'038193445566'),
(N'Bùi Thị Hải', 27, N'Nữ', N'Kế Toán', 10000000, N'0924234567', N'hai.bt@hotel.com', N'040200556677'),
(N'Ngô Tấn Tài', 31, N'Nam', N'Bảo Vệ', 7500000, N'0935234567', N'tai.nt@hotel.com', N'042194667788'),
(N'Dương Hồng Ngọc', 23, N'Nữ', N'Lễ Tân', 8000000, N'0946234567', N'ngoc.dh@hotel.com', N'044201778899'),
(N'Lý Văn Thanh', 34, N'Nam', N'Kỹ Thuật', 9000000, N'0957234567', N'thanh.lv@hotel.com', N'046191889900'),
(N'Trịnh Thảo Nhi', 26, N'Nữ', N'Thu Ngân', 8500000, N'0968234567', N'nhi.tt@hotel.com', N'048200990011'),
(N'Mai Quốc Việt', 28, N'Nam', N'Pha Chế', 8000000, N'0979234567', N'viet.mq@hotel.com', N'050197001122'),
(N'Hà Minh Triết', 33, N'Nam', N'Bảo Vệ', 7500000, N'0980234567', N'triet.hm@hotel.com', N'052192112233'),
(N'Phan Thị Dung', 29, N'Nữ', N'Buồng Phòng', 7000000, N'0991234567', N'dung.pt@hotel.com', N'054196223344'),
(N'Cao Văn Hùng', 36, N'Nam', N'Quản Lý Kho', 11000000, N'0902234567', N'hung.cv@hotel.com', N'056189334455'),
(N'Võ Thị Loan', 30, N'Nữ', N'Phục Vụ', 6500000, N'0913234568', N'loan.vt@hotel.com', N'058195445566'),
(N'Lương Văn Hòa', 27, N'Nam', N'Phục Vụ', 6500000, N'0924234568', N'hoa.lv@hotel.com', N'060198556677'),
(N'Trương Thúy Kiều', 25, N'Nữ', N'Lễ Tân', 8200000, N'0935234568', N'kieu.tt@hotel.com', N'062200667788');

-- 4. Nạp dữ liệu bảng tai_khoan
INSERT INTO tai_khoan (ten_dang_nhap, mat_khau, vai_tro, email) VALUES
(N'admin', N'123456', N'ADMIN', N'admin@hotel.com'),
(N'letan', N'123456', N'RECEPTIONIST', N'letan@hotel.com'),
(N'letan1', N'123456', N'RECEPTIONIST', N'letan2@hotel.com');

-- 5. Nạp dữ liệu bảng dich_vu
INSERT INTO dich_vu (ten_dich_vu, gia_dich_vu) VALUES
(N'Nước suối tinh khiết', 20000),
(N'Nước ngọt có ga', 25000),
(N'Nước ép cam tươi', 40000),
(N'Nước ép táo', 40000),
(N'Nước ép dưa hấu', 45000),
(N'Cà phê rang xay', 30000),
(N'Trà thảo mộc', 25000),
(N'Bia chai lạnh', 35000),
(N'Rượu vang đỏ (Ly)', 120000),
(N'Rượu vang trắng (Ly)', 120000),
(N'Set trái cây nhiệt đới', 80000),
(N'Ăn sáng tại phòng', 70000),
(N'Giặt ủi quần áo', 50000),
(N'Dịch vụ đánh giày', 30000),
(N'Thuê xe máy (Ngày)', 150000),
(N'Thuê xe ô tô (Ngày)', 800000),
(N'Đưa đón sân bay (1 chiều)', 200000),
(N'Giữ hành lý miễn phí', 0),
(N'Wifi tốc độ cao miễn phí', 0),
(N'Bãi đỗ xe miễn phí', 0),
(N'Massage thư giãn toàn thân', 300000),
(N'Liệu trình Spa chăm sóc da', 500000),
(N'Thuê phòng họp (Giờ)', 500000),
(N'Tour tham quan địa phương', 600000),
(N'Dịch vụ gọi đồ ăn nhanh', 50000);

-- 6. Nạp dữ liệu bảng dat_phong
INSERT INTO dat_phong (ma_khach_hang, so_phong, ngay_nhan_phong, ngay_tra_phong, trang_thai) VALUES
(1, N'101', N'2026-10-01 08:00:00', NULL, N'Đang lưu trú'),
(2, N'103', N'2026-10-02 09:30:00', NULL, N'Đang lưu trú'),
(3, N'202', N'2026-10-03 14:00:00', NULL, N'Đang lưu trú'),
(4, N'205', N'2026-10-04 10:15:00', NULL, N'Đang lưu trú'),
(5, N'302', N'2026-10-05 11:00:00', NULL, N'Đang lưu trú'),
(6, N'305', N'2026-10-06 12:00:00', NULL, N'Đang lưu trú'),
(7, N'402', N'2026-10-07 15:30:00', NULL, N'Đang lưu trú'),
(8, N'502', N'2026-10-08 16:00:00', NULL, N'Đang lưu trú'),
(9, N'102', N'2026-09-01 08:00:00', N'2026-09-03 12:00:00', N'Đã trả phòng'),
(10, N'104', N'2026-09-02 09:00:00', N'2026-09-05 12:00:00', N'Đã trả phòng'),
(11, N'201', N'2026-09-05 10:00:00', N'2026-09-07 12:00:00', N'Đã trả phòng'),
(12, N'203', N'2026-09-08 14:00:00', N'2026-09-10 12:00:00', N'Đã trả phòng'),
(13, N'204', N'2026-09-10 11:00:00', N'2026-09-12 12:00:00', N'Đã trả phòng'),
(14, N'301', N'2026-09-12 13:00:00', N'2026-09-15 12:00:00', N'Đã trả phòng'),
(15, N'303', N'2026-09-15 15:00:00', N'2026-09-18 12:00:00', N'Đã trả phòng'),
(16, N'304', N'2026-09-18 09:00:00', N'2026-09-20 12:00:00', N'Đã trả phòng'),
(17, N'401', N'2026-09-20 10:00:00', N'2026-09-22 12:00:00', N'Đã trả phòng'),
(18, N'403', N'2026-09-22 14:00:00', N'2026-09-25 12:00:00', N'Đã trả phòng'),
(19, N'404', N'2026-09-25 11:00:00', N'2026-09-27 12:00:00', N'Đã trả phòng'),
(20, N'501', N'2026-09-27 16:00:00', N'2026-09-30 12:00:00', N'Đã trả phòng');

-- 7. Nạp dữ liệu bảng su_dung_dich_vu
INSERT INTO su_dung_dich_vu (so_phong, so_giay_to, ma_dich_vu, so_luong, thanh_tien, ngay_su_dung) VALUES
(N'101', N'001203001111', 1, 2, 40000, N'2026-10-01 09:00:00'),
(N'101', N'001203001111', 6, 1, 30000, N'2026-10-01 10:00:00'),
(N'103', N'079201002222', 2, 3, 75000, N'2026-10-02 10:30:00'),
(N'103', N'079201002222', 12, 1, 70000, N'2026-10-02 11:00:00'),
(N'202', N'038198003333', 3, 2, 80000, N'2026-10-03 15:00:00'),
(N'202', N'038198003333', 13, 1, 50000, N'2026-10-03 16:00:00'),
(N'205', N'025195004444', 8, 4, 140000, N'2026-10-04 18:00:00'),
(N'302', N'031200005555', 11, 1, 80000, N'2026-10-05 14:00:00'),
(N'305', N'049197006666', 21, 1, 300000, N'2026-10-06 17:00:00'),
(N'402', N'036189007777', 17, 1, 200000, N'2026-10-07 08:00:00'),
(N'102', N'064196009999', 1, 2, 40000, N'2026-09-01 10:00:00'),
(N'104', N'072199010000', 2, 2, 50000, N'2026-09-02 11:00:00'),
(N'201', N'051201011111', 6, 2, 60000, N'2026-09-05 14:00:00'),
(N'203', N'034200012222', 12, 2, 140000, N'2026-09-08 09:00:00'),
(N'204', N'075197013333', 15, 1, 150000, N'2026-09-10 15:00:00'),
(N'301', N'080202014444', 16, 1, 800000, N'2026-09-12 16:00:00'),
(N'303', N'084198015555', 21, 2, 600000, N'2026-09-15 18:00:00'),
(N'304', N'089201016666', 13, 2, 100000, N'2026-09-18 10:00:00'),
(N'401', N'091195017777', 22, 1, 500000, N'2026-09-20 11:00:00'),
(N'501', N'095197019999', 24, 1, 600000, N'2026-09-27 17:00:00');

-- 8. Nạp dữ liệu bảng hoa_don (20 bản ghi)
INSERT INTO hoa_don (ma_dat_phong, ngay_lap, tien_phong, tien_dich_vu, tong_tien, trang_thai) VALUES
(9,  N'2026-09-03 12:00:00', 1000000, 40000,  1040000, N'Đã thanh toán'),
(10, N'2026-09-05 12:00:00', 2100000, 50000,  2150000, N'Đã thanh toán'),
(11, N'2026-09-07 12:00:00', 2000000, 60000,  2060000, N'Đã thanh toán'),
(12, N'2026-09-10 12:00:00', 2400000, 140000, 2540000, N'Đã thanh toán'),
(13, N'2026-09-12 12:00:00', 2400000, 150000, 2550000, N'Đã thanh toán'),
(14, N'2026-09-15 12:00:00', 4500000, 800000, 5300000, N'Đã thanh toán'),
(15, N'2026-09-18 12:00:00', 5400000, 600000, 6000000, N'Đã thanh toán'),
(16, N'2026-09-20 12:00:00', 3600000, 100000, 3700000, N'Đã thanh toán'),
(17, N'2026-09-22 12:00:00', 5000000, 500000, 5500000, N'Đã thanh toán'),
(18, N'2026-09-25 12:00:00', 9000000, 0,      9000000, N'Đã thanh toán'),
(19, N'2026-09-27 12:00:00', 6000000, 0,      6000000, N'Đã thanh toán'),
(20, N'2026-09-30 12:00:00', 13500000, 600000, 14100000, N'Đã thanh toán'),
(1,  N'2026-10-02 08:00:00', 500000,  70000,  570000,  N'Đã thanh toán'),
(2,  N'2026-10-03 09:00:00', 700000,  145000, 845000,  N'Đã thanh toán'),
(3,  N'2026-10-04 10:00:00', 1000000, 130000, 1130000, N'Đã thanh toán'),
(4,  N'2026-10-05 11:00:00', 1000000, 140000, 1140000, N'Đã thanh toán'),
(5,  N'2026-10-06 12:00:00', 1500000, 80000,  1580000, N'Đã thanh toán'),
(6,  N'2026-10-07 13:00:00', 1500000, 300000, 1800000, N'Đã thanh toán'),
(7,  N'2026-10-08 14:00:00', 2500000, 200000, 2700000, N'Đã thanh toán'),
(8,  N'2026-10-09 15:00:00', 5000000, 0,      5000000, N'Đã thanh toán');

-- Link legacy usage rows to their original stay before adding new occupants.
-- The original seed assigned room 501 to the identity of the guest in room 404.
UPDATE su_dung_dich_vu SET so_giay_to = N'097202020000'
WHERE so_phong = N'501' AND so_giay_to = N'095197019999';
UPDATE u SET ma_dat_phong = d.ma_dat_phong,
    ten_dich_vu_luc_goi = v.ten_dich_vu,
    don_gia_luc_goi = v.gia_dich_vu,
    ma_yeu_cau = CONVERT(NVARCHAR(36), NEWID()), nguoi_goi = N'letan'
FROM su_dung_dich_vu u
JOIN dich_vu v ON v.ma_dich_vu = u.ma_dich_vu
JOIN khach_hang k ON k.so_giay_to = u.so_giay_to
JOIN dat_phong d ON d.ma_khach_hang = k.ma_khach_hang AND d.so_phong = u.so_phong;

-- Paid invoices describe completed stays. Keep active stays separate.
UPDATE d SET ngay_tra_phong = h.ngay_lap, trang_thai = N'Đã trả phòng'
FROM dat_phong d JOIN hoa_don h ON h.ma_dat_phong = d.ma_dat_phong;
-- Repair demo usage timestamps that originally predated check-in.
UPDATE u SET ngay_su_dung = DATEADD(MINUTE, 30, d.ngay_nhan_phong)
FROM su_dung_dich_vu u JOIN dat_phong d ON d.ma_dat_phong = u.ma_dat_phong
WHERE u.ngay_su_dung < d.ngay_nhan_phong;

INSERT INTO dat_phong(ma_khach_hang,so_phong,ngay_nhan_phong,trang_thai)
SELECT ma_khach_hang,so_phong,DATEADD(DAY,-1,SYSDATETIME()),N'Đang lưu trú'
FROM khach_hang WHERE so_phong IS NOT NULL;
ALTER TABLE su_dung_dich_vu ALTER COLUMN ma_dat_phong BIGINT NOT NULL;
ALTER TABLE su_dung_dich_vu ALTER COLUMN ten_dich_vu_luc_goi NVARCHAR(100) NOT NULL;
ALTER TABLE su_dung_dich_vu ALTER COLUMN don_gia_luc_goi DECIMAL(15,2) NOT NULL;
ALTER TABLE su_dung_dich_vu ALTER COLUMN ma_yeu_cau NVARCHAR(36) NOT NULL;
ALTER TABLE su_dung_dich_vu ALTER COLUMN nguoi_goi NVARCHAR(50) NOT NULL;
ALTER TABLE su_dung_dich_vu ADD CONSTRAINT uq_dichvu_request UNIQUE(ma_yeu_cau);
CREATE INDEX ix_sddv_luot_o ON su_dung_dich_vu(ma_dat_phong);
CREATE INDEX ix_sddv_dich_vu ON su_dung_dich_vu(ma_dich_vu);
COMMIT TRANSACTION;
GO
SELECT N'Database demo đã tạo. Tài khoản web: admin / 123456, letan / 123456.' AS ket_qua;
