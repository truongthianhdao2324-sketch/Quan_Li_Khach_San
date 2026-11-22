package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.SuDungDichVu;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.util.List;

public interface SuDungDichVuRepository extends JpaRepository<SuDungDichVu, Long> {
    // Khoản cũ chưa có mã lượt ở chỉ được lấy khi khớp duy nhất khách, phòng và thời gian.
    String DIEU_KIEN_LUOT_O = """
        where dp.maDatPhong = :stayId or (s.datPhong is null and exists (
            select d.maDatPhong from DatPhong d
            where d.maDatPhong = :stayId and d.soPhong = s.soPhong
              and d.khachHang.soGiayTo = s.soGiayTo
              and s.ngaySuDung >= d.ngayNhanPhong
              and (d.ngayTraPhong is null or s.ngaySuDung < d.ngayTraPhong)
              and not exists (
                  select khac.maDatPhong from DatPhong khac
                  where khac.maDatPhong <> d.maDatPhong and khac.soPhong = s.soPhong
                    and khac.khachHang.soGiayTo = s.soGiayTo
                    and s.ngaySuDung >= khac.ngayNhanPhong
                    and (khac.ngayTraPhong is null or s.ngaySuDung < khac.ngayTraPhong)
              )
        ))
        """;

    boolean existsByDichVuMaDichVu(Long maDichVu);

    @EntityGraph(attributePaths = "dichVu")
    @Query(value = "select s from SuDungDichVu s left join s.datPhong dp " + DIEU_KIEN_LUOT_O,
           countQuery = "select count(s) from SuDungDichVu s left join s.datPhong dp " + DIEU_KIEN_LUOT_O)
    Page<SuDungDichVu> findByDatPhongMaDatPhong(@Param("stayId") Long maDatPhong, Pageable phanTrang);

    @Query("select s from SuDungDichVu s join fetch s.dichVu left join s.datPhong dp "
            + DIEU_KIEN_LUOT_O + " order by s.ngaySuDung desc, s.id desc")
    List<SuDungDichVu> findLichSu(@Param("stayId") Long maDatPhong);

    @Query("select coalesce(sum(s.thanhTien), 0) from SuDungDichVu s left join s.datPhong dp " + DIEU_KIEN_LUOT_O)
    BigDecimal tongTienTheoLuotO(@Param("stayId") Long maDatPhong);
}
