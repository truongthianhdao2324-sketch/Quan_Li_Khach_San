package org.example.quan_li_khach_san.repository;

import org.example.quan_li_khach_san.entity.DatPhong;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;

public interface DatPhongRepository extends JpaRepository<DatPhong, Long> {
    @EntityGraph(attributePaths = {"khachHang", "phong"})
    @Query("select d from DatPhong d where d.ngayTraPhong is null and d.trangThai = 'Đang lưu trú' order by d.phong.soPhong")
    List<DatPhong> findDangLuuTru();
    @EntityGraph(attributePaths = {"khachHang", "phong"})
    Optional<DatPhong> findFirstBySoPhongAndTrangThaiOrderByNgayNhanPhongDesc(String soPhong, String trangThai);
    // Member 4 must acquire this same row lock before calculating a final bill / checkout.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DatPhong d where d.maDatPhong = :id")
    Optional<DatPhong> findLockedById(@Param("id") Long id);
}
