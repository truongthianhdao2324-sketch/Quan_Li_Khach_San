package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.SuDungDichVu;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SuDungDichVuRepository extends JpaRepository<SuDungDichVu, Long> {
    Optional<SuDungDichVu> findByMaYeuCau(String requestId);
    boolean existsByDichVuMaDichVu(Long serviceId);
    Page<SuDungDichVu> findByDatPhongMaDatPhong(Long stayId, Pageable pageable);
    @Query("select s from SuDungDichVu s join fetch s.dichVu join fetch s.datPhong where s.datPhong.maDatPhong = :stayId order by s.ngaySuDung desc, s.id desc")
    List<SuDungDichVu> findLichSu(@Param("stayId") Long stayId);
    @Query("select coalesce(sum(s.thanhTien), 0) from SuDungDichVu s where s.datPhong.maDatPhong = :stayId")
    BigDecimal tongTienTheoLuotO(@Param("stayId") Long stayId);
    @Modifying
    @Query("update SuDungDichVu s set s.daGuiThongBao = true where s.id = :id")
    int markNotified(@Param("id") Long id);
}
