"""Generate a NEW SQL Server demo script from the unmodified group MySQL seed.
Run only when deliberately regenerating the checked-in SQL script.
"""
from pathlib import Path
import re

root = Path(__file__).resolve().parent.parent
source = root / 'qlks.sql'
text = source.read_text(encoding='utf-8')
text = text[text.index('-- 1. BẢNG PHÒNG'):]
text = text.replace('VARCHAR(', 'NVARCHAR(').replace('INT AUTO_INCREMENT PRIMARY KEY', 'BIGINT IDENTITY(1,1) PRIMARY KEY')
text = re.sub(r'\b(ma_khach_hang|ma_dat_phong|ma_dich_vu) INT\b', r'\1 BIGINT', text)
text = text.replace('DATETIME', 'DATETIME2(6)').replace('ON DELETE CASCADE', 'ON DELETE NO ACTION')
text = text.replace('gia_dich_vu DECIMAL(15, 2) NOT NULL\n', 'gia_dich_vu DECIMAL(15, 2) NOT NULL CHECK (gia_dich_vu >= 0),\n    dang_kinh_doanh BIT NOT NULL DEFAULT 1\n')
text = text.replace('so_luong INT DEFAULT 1,', 'so_luong INT NOT NULL DEFAULT 1 CHECK (so_luong BETWEEN 1 AND 999),')
text = text.replace('    thanh_tien DECIMAL(15, 2),', '''    ma_dat_phong BIGINT NULL,
    ten_dich_vu_luc_goi NVARCHAR(100) NULL,
    don_gia_luc_goi DECIMAL(15,2) NULL,
    ma_yeu_cau NVARCHAR(36) NULL,
    nguoi_goi NVARCHAR(50) NULL,
    da_gui_thong_bao BIT NOT NULL DEFAULT 0,
    thanh_tien DECIMAL(15, 2) NOT NULL,''')
text = text.replace('    FOREIGN KEY (ma_dich_vu) REFERENCES dich_vu(ma_dich_vu) ON DELETE NO ACTION', '''    FOREIGN KEY (ma_dich_vu) REFERENCES dich_vu(ma_dich_vu) ON DELETE NO ACTION,
    CONSTRAINT fk_sddv_luot_o FOREIGN KEY (ma_dat_phong) REFERENCES dat_phong(ma_dat_phong)''')
# SQL Server Unicode literals preserve the Vietnamese demo text.
text = re.sub(r"'([^']*)'", lambda m: "N'" + m[1] + "'", text)
prefix = '''-- NEW DATABASE ONLY. Run with sqlcmd -v DbName="QLKS_TV3" -b -i sql/01_create_demo.sql
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
'''
suffix = '''

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
'''
(root / 'sql').mkdir(exist_ok=True)
(root / 'sql/01_create_demo.sql').write_text(prefix + text + suffix, encoding='utf-8-sig')
print(root / 'sql/01_create_demo.sql')
