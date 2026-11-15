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

    // ===Tìm kiếm theo từ khóa (Mã HĐ dạng HD0001, Số phòng, Tên khách) & lọc trạng thái trực tiếp trên DB===
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

    // ==== Lấy danh sách hóa đơn theo trạng thái ====
    List<HoaDon> findByTrangThaiOrderByNgayLapDesc(String trangThai);

    // 3. Kiểm tra hóa đơn theo mã đặt phòng (tránh xuất trùng hóa đơn)
    Optional<HoaDon> findByMaDatPhong(Long maDatPhong);

    // ===Truy vấn hóa đơn theo số phòng====
    @Query("SELECT h FROM HoaDon h WHERE h.datPhong.soPhong = :soPhong ORDER BY h.ngayLap DESC")
    List<HoaDon> findBySoPhong(@Param("soPhong") String soPhong);

    // ===Tính tổng doanh thu các hóa đơn đã thanh toán (COALESCE tránh trả về null)===
    @Query("SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h WHERE h.trangThai = 'Đã thanh toán'")
    Double tinhTongDoanhThu();

    // ===Thống kê doanh thu theo khoảng thời gian =====
    @Query("""
        SELECT COALESCE(SUM(h.tongTien), 0) FROM HoaDon h
        WHERE h.trangThai = 'Đã thanh toán' AND h.ngayLap BETWEEN :tuNgay AND :denNgay
    """)
    Double tinhDoanhThuTheoThoiGian(@Param("tuNgay") LocalDateTime tuNgay,
                                    @Param("denNgay") LocalDateTime denNgay);

    // === Đếm số hóa đơn theo trạng thái====
    long countByTrangThai(String trangThai);

    // ==== Lấy tổng tiền dịch vụ phát sinh từ bảng dịch vụ  theo mã đặt phòng====

    @Query(value = "SELECT COALESCE(SUM(thanh_tien), 0) FROM su_dung_dich_vu WHERE ma_dat_phong = :maDatPhong", nativeQuery = true)
    Double tinhTongTienDichVuTheoDatPhong(@Param("maDatPhong") Long maDatPhong);
}