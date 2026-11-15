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

ALTER TABLE su_dung_dich_vu ADD COLUMN ma_dat_phong INT NULL;
