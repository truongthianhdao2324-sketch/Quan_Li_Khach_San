-- Chạy sau qlks.sql của nhóm, trên MySQL.
USE QLKS;

-- Lấy đúng kiểu mã đặt phòng đang có (INT trong SQL gốc).
SET @tv3_id_type = (
    SELECT COLUMN_TYPE FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dat_phong' AND COLUMN_NAME = 'ma_dat_phong'
);
SET @tv3_ddl = IF(
    (SELECT COUNT(*) FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'su_dung_dich_vu' AND COLUMN_NAME = 'ma_dat_phong') = 0,
    CONCAT('ALTER TABLE su_dung_dich_vu ADD COLUMN ma_dat_phong ', @tv3_id_type, ' NULL'),
    'SELECT ''ma_dat_phong da co'' AS ket_qua'
);
PREPARE tv3_stmt FROM @tv3_ddl;
EXECUTE tv3_stmt;
DEALLOCATE PREPARE tv3_stmt;

-- Chỉ ghép khoản cũ khi khách, phòng và thời điểm khớp đúng một lượt ở.
UPDATE su_dung_dich_vu u
JOIN (
    SELECT s.id, MIN(d.ma_dat_phong) AS ma_dat_phong
    FROM su_dung_dich_vu s
    JOIN khach_hang k ON k.so_giay_to = s.so_giay_to
    JOIN dat_phong d ON d.ma_khach_hang = k.ma_khach_hang AND d.so_phong = s.so_phong
      AND s.ngay_su_dung >= d.ngay_nhan_phong
      AND (d.ngay_tra_phong IS NULL OR s.ngay_su_dung < d.ngay_tra_phong)
    WHERE s.ma_dat_phong IS NULL
    GROUP BY s.id
    HAVING COUNT(*) = 1
) matched ON matched.id = u.id
SET u.ma_dat_phong = matched.ma_dat_phong;

-- Các dòng còn lại cần đối chiếu dữ liệu, không tự gán vào lượt ở gần nhất.
SELECT id, so_phong, so_giay_to, ngay_su_dung, thanh_tien
FROM su_dung_dich_vu WHERE ma_dat_phong IS NULL;
