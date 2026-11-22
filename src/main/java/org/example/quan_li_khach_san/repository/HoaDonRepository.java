package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {

    @Query("""
        SELECT h FROM HoaDon h
        LEFT JOIN h.datPhong dp
        LEFT JOIN dp.khachHang kh
        WHERE (:trangThai IS NULL OR :trangThai = '' OR LOWER(h.trangThai) = LOWER(:trangThai))
          AND (:tuKhoa IS NULL OR :tuKhoa = ''
               OR LOWER(CONCAT('HD', SUBSTRING(CAST(10000 + h.maHoaDon AS string), 2, 4))) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
               OR LOWER(dp.soPhong) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
               OR LOWER(kh.hoTen) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
        ORDER BY h.maHoaDon ASC
    """)
    List<HoaDon> timKiemHoaDon(@Param("tuKhoa") String tuKhoa, @Param("trangThai") String trangThai);

    List<HoaDon> findByTrangThaiOrderByNgayLapDesc(String trangThai);

    Optional<HoaDon> findByMaDatPhong(Long maDatPhong);

    @Query("SELECT h FROM HoaDon h WHERE h.datPhong.soPhong = :soPhong ORDER BY h.ngayLap DESC")
    List<HoaDon> findBySoPhong(@Param("soPhong") String soPhong);

    @Query("SELECT COALESCE(SUM(h.tienPhong + h.tienDichVu), 0) FROM HoaDon h WHERE h.trangThai = 'Đã thanh toán'")
    Double tinhTongDoanhThu();

    @Query("""
        SELECT COALESCE(SUM(h.tienPhong + h.tienDichVu), 0) FROM HoaDon h
        WHERE h.trangThai = 'Đã thanh toán' AND h.ngayLap BETWEEN :tuNgay AND :denNgay
    """)
    Double tinhDoanhThuTheoThoiGian(@Param("tuNgay") LocalDateTime tuNgay,
                                    @Param("denNgay") LocalDateTime denNgay);

    long countByTrangThai(String trangThai);

    @Query(value = """
        SELECT COALESCE(SUM(s.thanh_tien), 0)
        FROM su_dung_dich_vu s
        INNER JOIN dat_phong dp ON LTRIM(RTRIM(s.so_phong)) = LTRIM(RTRIM(dp.so_phong))
        INNER JOIN khach_hang kh ON dp.ma_khach_hang = kh.ma_khach_hang
                                AND LTRIM(RTRIM(s.so_giay_to)) = LTRIM(RTRIM(kh.so_giay_to))
        WHERE dp.ma_dat_phong = :maDatPhong
          AND CAST(s.ngay_su_dung AS DATE) >= CAST(dp.ngay_nhan_phong AS DATE)
          AND (dp.ngay_tra_phong IS NULL OR CAST(s.ngay_su_dung AS DATE) <= CAST(dp.ngay_tra_phong AS DATE))
    """, nativeQuery = true)
    Double tinhTongTienDichVuTheoDatPhong(@Param("maDatPhong") Long maDatPhong);
}