package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.DatPhong;
import org.example.quan_li_khach_san.entity.SuDungDichVu;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface SuDungDichVuRepository extends JpaRepository<SuDungDichVu, Long> {
    // SQL nhóm lưu khoản phát sinh bằng phòng, giấy tờ và thời điểm sử dụng.
    // Chỉ tính vào một lượt ở khi dữ liệu khớp duy nhất lượt đó.
    String DIEU_KIEN_LUOT_O = """
        where exists (
            select d.maDatPhong from DatPhong d
            where d.maDatPhong = :stayId and d.soPhong = s.soPhong
              and d.khachHang.soGiayTo = s.soGiayTo
              and s.ngaySuDung >= d.ngayNhanPhong
              and (d.ngayTraPhong is null or s.ngaySuDung <= d.ngayTraPhong)
              and not exists (
                  select khac.maDatPhong from DatPhong khac
                  where khac.maDatPhong <> d.maDatPhong and khac.soPhong = s.soPhong
                    and khac.khachHang.soGiayTo = s.soGiayTo
                    and s.ngaySuDung >= khac.ngayNhanPhong
                    and (khac.ngayTraPhong is null or s.ngaySuDung <= khac.ngayTraPhong)
              )
        )
        """;

    boolean existsByDichVuMaDichVu(Long maDichVu);

    @EntityGraph(attributePaths = "dichVu")
    @Query(value = "select s from SuDungDichVu s " + DIEU_KIEN_LUOT_O,
           countQuery = "select count(s) from SuDungDichVu s " + DIEU_KIEN_LUOT_O)
    Page<SuDungDichVu> findTheoLuotO(@Param("stayId") Long maDatPhong, Pageable phanTrang);

    @Query("select s from SuDungDichVu s join fetch s.dichVu "
            + DIEU_KIEN_LUOT_O + " order by s.ngaySuDung desc, s.id desc")
    List<SuDungDichVu> findLichSu(@Param("stayId") Long maDatPhong);

    @Query("select coalesce(sum(s.thanhTien), 0) from SuDungDichVu s " + DIEU_KIEN_LUOT_O)
    BigDecimal tongTienTheoLuotO(@Param("stayId") Long maDatPhong);

    @Query("""
        select d from DatPhong d
        where d.soPhong = :soPhong and d.khachHang.soGiayTo = :soGiayTo
          and :thoiDiem >= d.ngayNhanPhong
          and (d.ngayTraPhong is null or :thoiDiem <= d.ngayTraPhong)
        """)
    List<DatPhong> findLuotOPhuHop(@Param("soPhong") String soPhong,
                                 @Param("soGiayTo") String soGiayTo,
                                 @Param("thoiDiem") LocalDateTime thoiDiem);
}
