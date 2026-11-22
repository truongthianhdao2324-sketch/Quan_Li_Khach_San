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

    Optional<HoaDon> findFirstByMaDatPhongOrderByMaHoaDonAsc(Long maDatPhong);

    default Optional<HoaDon> findByMaDatPhong(Long maDatPhong) {
        return findFirstByMaDatPhongOrderByMaHoaDonAsc(maDatPhong);
    }

    long countByTrangThai(String trangThai);

    @Query("SELECT COALESCE(SUM(h.tongTien), 0.0) FROM HoaDon h WHERE h.trangThai = 'Đã thanh toán'")
    Double tinhTongDoanhThu();

    @Query("SELECT COALESCE(SUM(h.tongTien), 0.0) FROM HoaDon h " +
            "WHERE h.trangThai = 'Đã thanh toán' " +
            "AND h.ngayLap >= :tuNgay AND h.ngayLap <= :denNgay")
    Double tinhDoanhThuTheoThoiGian(@Param("tuNgay") LocalDateTime tuNgay,
                                    @Param("denNgay") LocalDateTime denNgay);

    @Query("SELECT COALESCE(SUM(CAST(s.thanhTien AS double)), 0.0) " +
            "FROM SuDungDichVu s " +
            "WHERE EXISTS (" +
            "    SELECT d.maDatPhong FROM DatPhong d " +
            "    WHERE d.maDatPhong = :maDatPhong " +
            "      AND d.soPhong = s.soPhong " +
            "      AND (s.soGiayTo IS NULL OR s.soGiayTo = '' OR d.khachHang.soGiayTo = s.soGiayTo) " +
            "      AND (s.ngaySuDung IS NULL OR s.ngaySuDung >= d.ngayNhanPhong) " +
            "      AND (d.ngayTraPhong IS NULL OR s.ngaySuDung IS NULL OR s.ngaySuDung <= d.ngayTraPhong)" +
            ")")
    Double tinhTongTienDichVuTheoDatPhong(@Param("maDatPhong") Long maDatPhong);

    @Query("SELECT h FROM HoaDon h " +
            "LEFT JOIN FETCH h.datPhong dp " +
            "LEFT JOIN FETCH dp.khachHang kh " +
            "WHERE (:trangThai IS NULL OR :trangThai = '' OR h.trangThai = :trangThai) " +
            "ORDER BY h.maHoaDon ASC")
    List<HoaDon> layDanhSachTheoTrangThai(@Param("trangThai") String trangThai);
}